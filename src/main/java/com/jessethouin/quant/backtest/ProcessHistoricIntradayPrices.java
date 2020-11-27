package com.jessethouin.quant.backtest;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.CurrencyPosition;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Broker;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;
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
        config.setBroker(Broker.ALPACA_TEST);

        Currency currency = new Currency();
        currency.setSymbol("USD");
        currency.setCurrencyType(CurrencyTypes.FIAT);

        CurrencyPosition currencyPosition = new CurrencyPosition();
        currencyPosition.setQuantity(config.getInitialCash());
        currencyPosition.setOpened(new Date());
        currencyPosition.setPrice(BigDecimal.ONE);
        currencyPosition.setBaseCurrency(currency);

        currency.getCurrencyPositions().add(currencyPosition);
        currency.setPortfolio(portfolio);

        Security security = new Security();
        security.setSymbol("AAPL");
        security.setCurrency(currency);

        portfolio.getCurrencies().add(currency);
        portfolio.getSecurities().add(security);

        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = intradayPrices.get(0);
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        Calc c = new Calc(security, config, price);
        for (int i = 0; i < intradayPrices.size(); i++) {
            price = intradayPrices.get(i);
            shortMAValue = Util.getMA(intradayPrices, previousShortMAValue, i, shortLookback, price);
            longMAValue = Util.getMA(intradayPrices, previousLongMAValue, i, longLookback, price);
            c.updateCalc(price, shortMAValue, longMAValue, portfolio);
            c.decide();

            LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", shortLookback, longLookback, lowRisk, highRisk, Util.getPortfolioValue(portfolio, currency, price), shortMAValue, longMAValue, price, i));
            previousShortMAValue = shortMAValue;
            previousLongMAValue = longMAValue;
        }

        BigDecimal portfolioValue = Util.getPortfolioValue(portfolio, currency, price);
        String msg = MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", shortLookback, longLookback, lowRisk, highRisk, portfolioValue);
        LOG.trace(msg);
        BacktestParameterCombos.updateBest(msg, portfolioValue);

        return portfolioValue;
    }

}
