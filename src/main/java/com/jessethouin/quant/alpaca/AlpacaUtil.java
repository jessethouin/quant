package com.jessethouin.quant.alpaca;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jessethouin.quant.beans.*;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.CurrencyType;
import net.jacobpeterson.alpaca.openapi.marketdata.model.Sort;
import net.jacobpeterson.alpaca.openapi.trader.model.*;
import net.jacobpeterson.alpaca.openapi.trader.ApiException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.*;

public class AlpacaUtil {
    private static final Logger LOG = LogManager.getLogger(AlpacaUtil.class);

    public static void reconcile(Portfolio portfolio) {
        try {
            reconcilePositions(portfolio);
            reconcileOrders(portfolio);
            String currency = ALPACA_ACCOUNT_API.getAccount().getCurrency();
            String cash = ALPACA_ACCOUNT_API.getAccount().getCash();
            if (cash != null) {
                reconcileCurrency(portfolio, currency, new BigDecimal(cash), CurrencyType.FIAT);
            }
        } catch (ApiException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    private static void reconcileOrders(Portfolio portfolio) throws ApiException {
        portfolio.getAlpacaOrders().clear();

        List<Order> orders = ALPACA_ORDERS_API.getAllOrders(OrderStatus.NEW.getValue(), 50, null, OffsetDateTime.now().toString(), Sort.ASC.getValue(), Boolean.TRUE, null, null);
        while (!orders.isEmpty()) {
            orders.forEach(AlpacaStreamProcessor::processRemoteOrder);
            OffsetDateTime submittedAt = orders.getLast().getSubmittedAt();
            if (submittedAt == null) {
                LOG.error("Order submitted at is null.");
                return;
            }
            OffsetDateTime newest = submittedAt.plus(1, ChronoUnit.MILLIS);
            orders = ALPACA_ORDERS_API.getAllOrders(OrderStatus.NEW.getValue(), 50, newest.toString(), OffsetDateTime.now().toString(), Sort.ASC.getValue(), Boolean.TRUE, null, null);
        }
    }

    private static void reconcilePositions(Portfolio portfolio) throws ApiException {
        portfolio.getCurrencies().stream().filter(currency -> !currency.getSymbol().equals("USD")).forEach(currency -> {
            currency.getCurrencyLedgers().clear();
            currency.setQuantity(BigDecimal.ZERO);
            currency.setAvgCostBasis(BigDecimal.ZERO);
        });

        portfolio.getSecurities().forEach(security -> {
            security.getSecurityPosition().setQuantity(BigDecimal.ZERO);
            security.getSecurityPosition().setPrice(BigDecimal.ZERO);
            security.getSecurityPosition().setOpened(new Date());
        });

        ALPACA_POSITIONS_API.getAllOpenPositions().forEach(remotePosition -> {
            String remoteSymbol = remotePosition.getSymbol();
            BigDecimal remoteQuantity = new BigDecimal(remotePosition.getQty());

            if (remotePosition.getAssetClass().equals(AssetClass.CRYPTO)) {
                remoteSymbol = parseAlpacaCryptoSymbol(remoteSymbol);
                reconcileCurrency(portfolio, remoteSymbol, remoteQuantity, CurrencyType.CRYPTO);
            } else if (remotePosition.getAssetClass().equals(AssetClass.US_EQUITY)) {
                reconcileSecurity(portfolio, remotePosition);
            }
        });
    }

    private static void reconcileCurrency(Portfolio portfolio, String remoteSymbol, BigDecimal remoteQuantity, CurrencyType currencyType) {
        Currency currency = Util.getCurrencyFromPortfolio(remoteSymbol, portfolio, currencyType);

        LOG.info("{}: Reconciling local currency ledger with remote ledger ({}).", currency.getSymbol(), remoteQuantity);
        currency.setQuantity(remoteQuantity);
        currency.getCurrencyLedgers().clear();
        CurrencyLedger currencyLedger = CurrencyLedger.builder().currency(currency).credit(remoteQuantity).timestamp(new Date()).memo("Reconciling with Alpaca").build();
        currency.getCurrencyLedgers().add(currencyLedger);
    }

    private static void reconcileSecurity(Portfolio portfolio, Position remotePosition) {
        Security security = Util.getSecurityFromPortfolio(remotePosition.getSymbol(), portfolio);
        SecurityPosition securityPosition = security.getSecurityPosition();
        BigDecimal remoteQuantity = new BigDecimal(remotePosition.getQty());
        BigDecimal remotePrice = new BigDecimal(remotePosition.getAvgEntryPrice());

        LOG.info("{}: Reconciling local security ledger with remote ledger ({}).", security.getSymbol(), remoteQuantity);
        securityPosition.setOpened(new Date());
        securityPosition.setPrice(remotePrice);
        securityPosition.setQuantity(remoteQuantity);
    }

    public static void showAlpacaAccountInfo() {
        Account alpacaAccount = null;
        try {
            alpacaAccount = ALPACA_ACCOUNT_API.getAccount();
        } catch (ApiException e) {
            LOG.error(e.getLocalizedMessage());
        }

        if (alpacaAccount != null) {
            LOG.info("\n\nAlpaca Account Information:");
            LOG.info("\t{}", alpacaAccount.toString().replace(",", ",\n\t"));
        }
    }

    static List<Order> getFilledOrders() {
        try {
            ArrayList<String> symbols = new ArrayList<>();
            ALPACA_POSITIONS_API.getAllOpenPositions().forEach(p -> symbols.add(p.getSymbol()));
            String commaSeparatedSymbols = String.join(",", symbols);
            return ALPACA_ORDERS_API.getAllOrders(OrderStatus.FILLED.getValue(), 500, OffsetDateTime.now().truncatedTo(ChronoUnit.DAYS).toString(), OffsetDateTime.now().toString(), Sort.ASC.getValue(), false, commaSeparatedSymbols, null);
        } catch (ApiException e) {
            LOG.error(e.getLocalizedMessage());
        }
        return null;
    }

    static Position getOpenPosition(String symbol) {
        Position position = null;
        try {
            position = ALPACA_POSITIONS_API.getOpenPosition(symbol);
        } catch (ApiException e) {
            String responseBody = e.getResponseBody();
            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonObject.get("code").getAsInt() == 40410000) {
                LOG.info("No open positions found for {}", symbol);
            } else {
                LOG.error("Error when searching positions for {}. {}", symbol, e.getLocalizedMessage());
            }
        }
        return position;
    }

    public static String parseAlpacaCryptoSymbol(String symbol) {
        if (symbol.endsWith("/USD")) {
            return symbol.substring(0, symbol.length() - 4);
        } else {
            return symbol;
        }
    }
}
