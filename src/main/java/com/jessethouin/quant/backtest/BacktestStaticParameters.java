package com.jessethouin.quant.backtest;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;

public class BacktestStaticParameters extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestStaticParameters.class);

    public static void runBacktest() {
        populateIntradayPrices();

        Portfolio portfolio = Util.createPortfolio();

        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = intradayPrices.get(0);
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        BigDecimal previousValue = BigDecimal.ZERO;

        Calc c;
        switch (CONFIG.getBroker()) {
            case ALPACA_TEST -> {
                Security aapl = Util.getSecurity(portfolio, "AAPL");
                c = new Calc(aapl, CONFIG, price);
            }
            case BINANCE_TEST -> {
                Currency base = Util.getCurrency(portfolio, "BTC");
                Currency counter = Util.getCurrency(portfolio, "USDT");
                c = new Calc(base, counter, CONFIG, BigDecimal.ZERO);
            }
            default -> throw new IllegalStateException("Unexpected value: " + CONFIG.getBroker());
        }

        for (int i = 0; i < intradayPrices.size(); i++) {
            price = intradayPrices.get(i);
            shortMAValue = Util.getMA(intradayPrices, previousShortMAValue, i, CONFIG.getShortLookback(), price);
            longMAValue = Util.getMA(intradayPrices, previousLongMAValue, i, CONFIG.getLongLookback(), price);
            c.updateCalc(price, shortMAValue, longMAValue);

            switch (CONFIG.getBroker()) {
                case ALPACA_TEST -> LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,000000.000}", CONFIG.getShortLookback(), CONFIG.getLongLookback(), CONFIG.getLowRisk(), CONFIG.getHighRisk(), Util.getPortfolioValue(portfolio, c.getSecurity().getCurrency(), price), shortMAValue, longMAValue, price, i));
                case BINANCE_TEST -> LOG.info("{} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}", i, shortMAValue, longMAValue, c.getLow(), c.getHigh(), price, Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity()));
            }

            c.decide();

            BigDecimal value = Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity());
            if (value.compareTo(previousValue.multiply(CONFIG.getStopLoss())) < 0) {
                LOG.warn("Invoking stop loss.");
                Transactions.placeCurrencySellOrder(CONFIG.getBroker(), c.getBase(), c.getCounter(), price);
                System.exit(69); // NICE
            }

            previousShortMAValue = shortMAValue;
            previousLongMAValue = longMAValue;
            previousValue = value;
        }

        AbstractBacktest.logMarketChange(intradayPrices.get(intradayPrices.size() - 1), intradayPrices.get(0), LOG);

        switch (CONFIG.getBroker()) {
            case ALPACA_TEST -> c.getSecurity().getSecurityPositions().forEach(ps -> LOG.info(ps.getPrice() + ", " + ps.getQuantity() + " : " + ps.getPrice().multiply(ps.getQuantity())));
            case BINANCE_TEST -> {
                LOG.info("base   : value: {}", Util.formatFiat(c.getBase().getQuantity()));
                LOG.info("counter: value: {}", Util.formatFiat(c.getCounter().getQuantity()));
                LOG.info("orders : {}", portfolio.getBinanceLimitOrders().size());
                LOG.info("fees   : {}", Util.formatFiat(portfolio.getBinanceLimitOrders().stream().map(BinanceLimitOrder::getFee).reduce(BigDecimal.ZERO, BigDecimal::add)));
            }
        }

        BigDecimal portfolioValue = BigDecimal.ZERO;
        switch (CONFIG.getBroker()) {
            case ALPACA_TEST -> portfolioValue = Util.getPortfolioValue(portfolio, c.getBase(), price);
            case BINANCE_TEST -> portfolioValue = Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity());
        }
        LOG.debug(MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4}", CONFIG.getShortLookback(), CONFIG.getLongLookback(), CONFIG.getLowRisk(), CONFIG.getHighRisk(), Util.formatFiat(portfolioValue)));
    }
}
