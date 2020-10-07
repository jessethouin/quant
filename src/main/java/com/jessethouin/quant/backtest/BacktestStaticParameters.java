package com.jessethouin.quant.backtest;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.exceptions.CashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;

public class BacktestStaticParameters extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestStaticParameters.class);

    public static void runBacktest(String[] args) {
        populateIntradayPrices();

        Config config = new Config();

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
            shortMAValue = Transactions.getMA(intradayPrices, previousShortMAValue, i, config.getShortLookback(), price);
            longMAValue = Transactions.getMA(intradayPrices, previousLongMAValue, i, config.getLongLookback(), price);
            c.updateCalc(price, shortMAValue, longMAValue, portfolio);
            try {
                Transactions.addCash(portfolio, c.decide());
            } catch (CashException e) {
                LOG.error(e);
            }

            LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), Transactions.getPortfolioValue(portfolio, security.getSymbol(), price), shortMAValue, longMAValue, price, i));
            previousShortMAValue = shortMAValue;
            previousLongMAValue = longMAValue;
        }

        BigDecimal portfolioValue = Transactions.getPortfolioValue(portfolio, security.getSymbol(), price);
        String msg = MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), portfolioValue);
        LOG.debug(msg);
    }
}
