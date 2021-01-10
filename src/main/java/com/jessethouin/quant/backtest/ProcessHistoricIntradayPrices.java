package com.jessethouin.quant.backtest;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.Callable;

public class ProcessHistoricIntradayPrices implements Callable<Object> {
    private static final Logger LOG = LogManager.getLogger(ProcessHistoricIntradayPrices.class);
    final int shortLookback;
    final int longLookback;
    final BigDecimal highRisk;
    final BigDecimal lowRisk;
    final List<BigDecimal> intradayPrices;

    public ProcessHistoricIntradayPrices(int shortLookback, int longLookback, BigDecimal highRisk, BigDecimal lowRisk, List<BigDecimal> intradayPrices) {
        this.shortLookback = shortLookback;
        this.longLookback = longLookback;
        this.highRisk = highRisk;
        this.lowRisk = lowRisk;
        this.intradayPrices = intradayPrices;
    }

    @Override
    public BigDecimal call() {
        Config config = Config.getTheadSafeConfig();
        config.setShortLookback(shortLookback);
        config.setLongLookback(longLookback);
        config.setLowRisk(lowRisk);
        config.setHighRisk(highRisk);

        Portfolio portfolio = Util.createPortfolio();

        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = intradayPrices.get(0);
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        BigDecimal previousValue = BigDecimal.ZERO;

        Calc c;
        switch (config.getBroker()) {
            case ALPACA_TEST -> {
                Security aapl = Util.getSecurity(portfolio, "AAPL");
                c = new Calc(aapl, config, price);
            }
            case BINANCE_TEST -> {
                Currency base = Util.getCurrency(portfolio, "BTC");
                Currency counter = Util.getCurrency(portfolio, "USDT");
                c = new Calc(base, counter, config, BigDecimal.ZERO);
            }
            default -> throw new IllegalStateException("Unexpected value: " + config.getBroker());
        }

        for (int i = 0; i < intradayPrices.size(); i++) {
            try {
                price = intradayPrices.get(i);
                shortMAValue = Util.getMA(intradayPrices, previousShortMAValue, i, shortLookback, price);
                longMAValue = Util.getMA(intradayPrices, previousLongMAValue, i, longLookback, price);
                c.updateCalc(price, shortMAValue, longMAValue, portfolio);

                switch (config.getBroker()) {
                    case ALPACA_TEST -> LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,000000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), Util.getPortfolioValue(portfolio, c.getSecurity().getCurrency(), price), shortMAValue, longMAValue, price, i));
                    case BINANCE_TEST -> LOG.trace("{} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}", i, shortMAValue, longMAValue, c.getLow(), c.getHigh(), price, Util.getBalance(portfolio, c.getBase(), c.getCounter(), price).add(Util.getBalance(portfolio, c.getCounter())));
                }

                c.decide();

                BigDecimal value = Util.getBalance(portfolio, c.getBase(), c.getCounter(), price).add(Util.getBalance(portfolio, c.getCounter()));
                if (value.compareTo(previousValue.multiply(config.getStopLoss())) < 0) {
                    Transactions.placeCurrencySellOrder(config.getBroker(), c.getBase(), c.getCounter(), price, true);
                    System.exit(69); // NICE
                }

                previousShortMAValue = shortMAValue;
                previousLongMAValue = longMAValue;
                previousValue = value;
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }

        BigDecimal portfolioValue = BigDecimal.ZERO;
        switch (config.getBroker()) {
            case ALPACA_TEST -> portfolioValue = Util.getPortfolioValue(portfolio, c.getBase(), price);
            case BINANCE_TEST -> portfolioValue = Util.getBalance(portfolio, c.getBase(), c.getCounter(), price)
                    .add(Util.getBalance(portfolio, c.getCounter()))
                    .subtract(portfolio.getBinanceLimitOrders().stream().map(BinanceLimitOrder::getFee).reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        String msg = MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", shortLookback, longLookback, lowRisk, highRisk, portfolioValue);
        LOG.trace(msg);
        BacktestParameterCombos.updateBest(msg, portfolioValue);

        return portfolioValue;
    }
}
