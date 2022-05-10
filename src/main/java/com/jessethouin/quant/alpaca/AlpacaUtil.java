package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.beans.*;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.AssetClassTypes;
import com.jessethouin.quant.conf.CurrencyTypes;
import net.jacobpeterson.alpaca.model.endpoint.account.Account;
import net.jacobpeterson.alpaca.model.endpoint.common.enums.SortDirection;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.CurrentOrderStatus;
import net.jacobpeterson.alpaca.model.endpoint.positions.Position;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
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
            reconcileCurrency(portfolio, ALPACA_ACCOUNT_API.get().getCurrency(), new BigDecimal(ALPACA_ACCOUNT_API.get().getCash()), CurrencyTypes.FIAT);
        } catch (AlpacaClientException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    private static void reconcileOrders(Portfolio portfolio) throws AlpacaClientException {
        portfolio.getAlpacaOrders().clear();

        List<Order> orders = ALPACA_ORDERS_API.get(CurrentOrderStatus.OPEN, 50, null, ZonedDateTime.now(), SortDirection.ASCENDING, Boolean.TRUE, null);
        while (orders.size() > 0) {
            orders.forEach(AlpacaStreamProcessor::processRemoteOrder);
            ZonedDateTime newest = orders.get(orders.size() - 1).getSubmittedAt().plus(1, ChronoUnit.MILLIS);
            orders = ALPACA_ORDERS_API.get(CurrentOrderStatus.OPEN, 50, newest, ZonedDateTime.now(), SortDirection.ASCENDING, Boolean.TRUE, null);
        }
    }

    private static void reconcilePositions(Portfolio portfolio) throws AlpacaClientException {
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

        ALPACA_POSITIONS_API.get().forEach(remotePosition -> {
            String remoteSymbol = remotePosition.getSymbol();
            BigDecimal remoteQuantity = new BigDecimal(remotePosition.getQuantity());

            AssetClassTypes assetClassType = AssetClassTypes.get(remotePosition.getAssetClass());

            if (assetClassType.equals(AssetClassTypes.CRYPTO)) {
                remoteSymbol = parseAlpacaCryptoSymbol(remoteSymbol);
                reconcileCurrency(portfolio, remoteSymbol, remoteQuantity, CurrencyTypes.CRYPTO);
            } else if (assetClassType.equals(AssetClassTypes.US_EQUITY)) {
                reconcileSecurity(portfolio, remotePosition);
            }
        });
    }

    private static void reconcileCurrency(Portfolio portfolio, String remoteSymbol, BigDecimal remoteQuantity, CurrencyTypes currencyType) {
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
        BigDecimal remoteQuantity = new BigDecimal(remotePosition.getQuantity());
        BigDecimal remotePrice = new BigDecimal(remotePosition.getAverageEntryPrice());

        LOG.info("{}: Reconciling local security ledger with remote ledger ({}).", security.getSymbol(), remoteQuantity);
        securityPosition.setOpened(new Date());
        securityPosition.setPrice(remotePrice);
        securityPosition.setQuantity(remoteQuantity);
    }

    public static void showAlpacaAccountInfo() {
        Account alpacaAccount = null;
        try {
            alpacaAccount = ALPACA_ACCOUNT_API.get();
        } catch (AlpacaClientException e) {
            LOG.error(e.getLocalizedMessage());
        }

        if (alpacaAccount != null) {
            LOG.info("\n\nAlpaca Account Information:");
            LOG.info("\t" + alpacaAccount.toString().replace(",", ",\n\t"));
        }
    }

    static List<Order> getFilledOrders() {
        try {
            ArrayList<String> symbols = new ArrayList<>();
            ALPACA_POSITIONS_API.get().forEach(p -> symbols.add(p.getSymbol()));
            return ALPACA_ORDERS_API.get(CurrentOrderStatus.CLOSED, 500, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS), ZonedDateTime.now(), SortDirection.ASCENDING, false, symbols);
        } catch (AlpacaClientException e) {
            LOG.error(e.getLocalizedMessage());
        }
        return null;
    }

    static Position getOpenPosition(String symbol) {
        Position position = null;
        try {
            position = ALPACA_POSITIONS_API.getBySymbol(symbol);
        } catch (AlpacaClientException e) {
            if (e.getAPIResponseCode() != null && e.getAPIResponseCode() == 40410000) {
                LOG.info("No open positions found for {}", symbol);
            } else {
                LOG.error("Error when searching positions for {}. {}", symbol, e.getLocalizedMessage());
            }
        }
        return position;
    }

    public static String parseAlpacaCryptoSymbol(String symbol) {
        if (symbol.length() > 3 && symbol.endsWith("USD")) {
            return symbol.substring(0, symbol.length() - 3);
        } else {
            return symbol;
        }
    }
}
