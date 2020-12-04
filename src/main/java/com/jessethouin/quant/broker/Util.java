package com.jessethouin.quant.broker;

import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.CurrencyPosition;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.binance.BinanceLive;
import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.calculators.SMA;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyTypes;
import com.jessethouin.quant.conf.MATypes;
import com.jessethouin.quant.db.Database;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.polygon.rest.exception.PolygonAPIRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.exceptions.CurrencyPairNotValidException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Util {
    private static final Logger LOG = LogManager.getLogger(Util.class);

    public static BigDecimal getPortfolioValue(Portfolio portfolio, Currency currency) {
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        holdings.updateAndGet(v -> v.add(getBalance(portfolio, currency)));
        portfolio.getSecurities().stream().filter(security -> security.getCurrency().equals(currency)).forEach(s -> {
            try {
                BigDecimal price = BigDecimal.valueOf(AlpacaLive.getInstance().getPolygonAPI().getLastQuote(s.getSymbol()).getLast().getAskprice());
                s.getSecurityPositions().forEach(
                        position -> holdings.updateAndGet(
                                v -> v.add(price.multiply(position.getQuantity()))));
            } catch (PolygonAPIRequestException e) {
                LOG.error(e.getLocalizedMessage());
            }
        });
        return holdings.get();
    }

    public static BigDecimal getPortfolioValue(Portfolio portfolio, Currency currency, BigDecimal price) {
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        holdings.updateAndGet(v -> v.add(getBalance(portfolio, currency)));
        portfolio.getSecurities().stream()
                .filter(security -> security.getCurrency().equals(currency))
                .forEach(security -> security.getSecurityPositions()
                        .forEach(position -> holdings.updateAndGet(v -> v.add(price.multiply(position.getQuantity())))));
        return holdings.get();
    }

    public static BigDecimal getBalance(Portfolio portfolio, Currency currency) {
        final BigDecimal[] balance = {BigDecimal.ZERO};
        portfolio.getCurrencies().stream()
                .filter(c -> c.equals(currency))
                .forEach(c -> c.getCurrencyPositions()
                        .forEach(currencyPosition -> balance[0] = balance[0].add(currencyPosition.getQuantity())));
        return balance[0];
    }

    public static BigDecimal getHeldSecurity(Security security) {
        BigDecimal[] v = {BigDecimal.ZERO};
        security.getSecurityPositions().forEach(securityPosition -> v[0] = v[0].add(securityPosition.getQuantity()));
        return v[0];
    }

    public static Set<CurrencyPair> getHeldCurrencyPairs(Portfolio portfolio) {
        Set<CurrencyPair> currencyPairs = new HashSet<>();
        portfolio.getCurrencies().forEach(currency -> currency.getCurrencyPositions().forEach(currencyPosition -> {
            CurrencyPair currencyPair = new CurrencyPair(currencyPosition.getBaseCurrency().getSymbol(), currencyPosition.getCounterCurrency().getSymbol());
            currencyPairs.add(currencyPair);
        }));
        return currencyPairs;
    }

    public static List<CurrencyPair> getAllCurrencyPairs(Config config) {
        List<CurrencyPair> currencyPairs = new ArrayList<>();
        config.getCryptoCurrencies().forEach(base -> config.getCryptoCurrencies().forEach(counter -> {
            if (!base.equals(counter) && !currencyPairs.contains(new CurrencyPair(counter, base))) {
                currencyPairs.add(new CurrencyPair(base, counter));
            }
        }));
        return currencyPairs;
    }

    public static BigDecimal getBudget(Portfolio portfolio, BigDecimal price, BigDecimal allowance, Currency currency, Security security) {
        int scale = security == null ? 8 : 0;
        return getBalance(portfolio, currency).multiply(allowance).divide(price, scale, RoundingMode.FLOOR);
    }

    public static BigDecimal getMA(List<BigDecimal> intradayPrices, BigDecimal previousMA, int i, int lookback, BigDecimal price) {
        BigDecimal ma;
        if (i < lookback) ma = BigDecimal.ZERO;
        else if (i == lookback) ma = SMA.sma(intradayPrices.subList(0, i), lookback);
        else ma = MA.ma(price, previousMA, lookback, MATypes.TEMA);
        return ma;
    }

    /**
     * If a Security with the symbol exists in the portfolio, returns that Security, otherwise creates a new Security,
     * adds to portfolio, and returnes the new Security.
     *
     * @param symbol    Stock symbol
     * @param portfolio Active portfolio
     * @return A Security object, either existing or new
     */
    public static Security getSecurityFromPortfolio(String symbol, Portfolio portfolio) {
        Optional<Security> s = portfolio.getSecurities().stream().filter(security -> security.getSymbol().equals(symbol)).findFirst();

        if (s.isPresent()) {
            return s.get();
        } else {
            Security security = new Security();
            security.setSymbol(symbol);
            security.setPortfolio(portfolio);
            Database.persistPortfolio(portfolio);
            security.setCurrency(getCurrencyFromPortfolio("USD", portfolio)); // todo: find a dynamic way to get currency of a security.
            return security;
        }
    }

    /**
     * If a Currency with the symbol exists in the portfolio, returns that Currency, otherwise creates a new Currency,
     * adds to portfolio, and returnes the new Currency.
     *
     * @param symbol    Currency symbol
     * @param portfolio Active portfolio
     * @return A Currency object, either existing or new
     */
    public static Currency getCurrencyFromPortfolio(String symbol, Portfolio portfolio) {
        Optional<Currency> c = portfolio.getCurrencies().stream().filter(currency -> currency.getSymbol().equals(symbol)).findFirst();

        if (c.isPresent()) {
            return c.get();
        } else {
            Currency currency = new Currency();
            currency.setCurrencyType(CurrencyTypes.FIAT);
            currency.setSymbol(symbol);
            currency.setPortfolio(portfolio);
            Database.persistPortfolio(portfolio);
            return currency;
        }
    }

    public static BigDecimal getCurrencyPositionValue(CurrencyPosition currencyPosition, Currency targetCounter) {
        Currency thisCounter = currencyPosition.getCounterCurrency();
        BigDecimal thisPrice = currencyPosition.getPrice();

        if (thisCounter.equals(targetCounter)) {
            return thisPrice;
        } else {
            String thisBase = currencyPosition.getBaseCurrency().getSymbol();
            BigDecimal priceWithThisCounter = getTickerPrice(thisBase, thisCounter.getSymbol());
            BigDecimal priceWithTargetCounter = getTickerPrice(thisBase, targetCounter.getSymbol());
            if (priceWithThisCounter != null) {
                BigDecimal change = thisPrice.divide(priceWithThisCounter, 8, RoundingMode.FLOOR);
                return priceWithTargetCounter.multiply(change);
            }
            return priceWithTargetCounter;
        }
    }

    public static BigDecimal getTickerPrice(String base, String counter) {
        BigDecimal ret = null;
        try {
            ret = BinanceLive.INSTANCE.getBinanceExchange().getMarketDataService().getTicker(new CurrencyPair(base, counter)).getLast();
        } catch (IOException e) {
            LOG.error(e.getMessage());
        } catch (CurrencyPairNotValidException e) {
            LOG.info("Currency Pair {}/{} is not valid. Swapping.", base, counter);
        }

        if (ret == null) {
            try {
                ret = BinanceLive.INSTANCE.getBinanceExchange().getMarketDataService().getTicker(new CurrencyPair(counter, base)).getLast();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            } catch (CurrencyPairNotValidException e) {
                LOG.info("Currency Pair {}/{} is not valid even after swapping. Return value is NULL, sorry.", counter, base);
            }
        }
        return ret;
    }

    public static void updateAlpacaOrder(AlpacaOrder alpacaOrder, Order order) {
        alpacaOrder.setClientOrderId(order.getClientOrderId());
        alpacaOrder.setUpdatedAt(order.getUpdatedAt());
        alpacaOrder.setSubmittedAt(order.getSubmittedAt());
        alpacaOrder.setFilledAt(order.getFilledAt());
        alpacaOrder.setExpiredAt(order.getExpiredAt());
        alpacaOrder.setCanceledAt(order.getCanceledAt());
        alpacaOrder.setFailedAt(order.getFailedAt());
        alpacaOrder.setReplacedAt(order.getReplacedAt());
        alpacaOrder.setReplacedBy(order.getReplacedBy());
        alpacaOrder.setReplaces(order.getReplaces());
        alpacaOrder.setAssetId(order.getAssetId());
        alpacaOrder.setSymbol(order.getSymbol());
        alpacaOrder.setAssetClass(order.getAssetClass());
        alpacaOrder.setQty(order.getQty());
        alpacaOrder.setFilledQty(order.getFilledQty());
        alpacaOrder.setType(order.getType());
        alpacaOrder.setSide(order.getSide());
        alpacaOrder.setTimeInForce(order.getTimeInForce());
        alpacaOrder.setLimitPrice(order.getLimitPrice());
        alpacaOrder.setStopPrice(order.getStopPrice());
        alpacaOrder.setFilledAvgPrice(order.getFilledAvgPrice());
        alpacaOrder.setStatus(order.getStatus());
        alpacaOrder.setExtendedHours(order.getExtendedHours());
        alpacaOrder.setLegs(order.getLegs());
        alpacaOrder.setTrailPrice(order.getTrailPrice());
        alpacaOrder.setTrailPercent(order.getTrailPercent());
        alpacaOrder.setHwm(order.getHwm());
        Database.persistAlpacaOrder(alpacaOrder);
    }

}
