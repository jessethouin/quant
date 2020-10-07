package com.jessethouin.quant.backtest;

import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.exceptions.CashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
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
        Config config = new Config();
        config.setShortLookback(shortLookback);
        config.setLongLookback(longLookback);
        config.setLowRisk(lowRisk);
        config.setHighRisk(highRisk);

        Portfolio portfolio = new Portfolio();
        portfolio.setCash(config.getInitialCash());

        Security security = new Security();
        security.setSymbol("AAPL");
        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(security)));

        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = intradayPrices.get(0);
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        Calc c = new Calc(security, config, price);
        for (int i = 0; i < intradayPrices.size(); i++) {
            price = intradayPrices.get(i);
            shortMAValue = Transactions.getMA(intradayPrices, previousShortMAValue, i, shortLookback, price);
            longMAValue = Transactions.getMA(intradayPrices, previousLongMAValue, i, longLookback, price);
            c.updateCalc(price, shortMAValue, longMAValue, portfolio);
            try {
                Transactions.addCash(portfolio, c.decide());
            } catch (CashException e) {
                LOG.error(e);
            }

            LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", shortLookback, longLookback, lowRisk, highRisk, Transactions.getPortfolioValue(portfolio, security.getSymbol(), price), shortMAValue, longMAValue, price, i));
            previousShortMAValue = shortMAValue;
            previousLongMAValue = longMAValue;
        }

        BigDecimal portfolioValue = Transactions.getPortfolioValue(portfolio, security.getSymbol(), price);
        String msg = MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", shortLookback, longLookback, lowRisk, highRisk, portfolioValue);
        LOG.debug(msg);
        BacktestParameterCombos.updateBest(msg, portfolioValue);

        return portfolioValue;
    }

}
