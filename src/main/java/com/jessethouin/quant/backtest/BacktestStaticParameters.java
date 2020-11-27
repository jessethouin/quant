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
import com.jessethouin.quant.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

public class BacktestStaticParameters extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestStaticParameters.class);

    public static void runBacktest() {
        populateIntradayPrices();

        Config config = new Config();
        config.setBroker(Broker.ALPACA_TEST);

        Portfolio portfolio = Database.getPortfolio();
        Security security;

        if (portfolio == null) {
            portfolio = new Portfolio();

            Currency currency = new Currency();
            currency.setSymbol("USD");
            currency.setCurrencyType(CurrencyTypes.FIAT);

            CurrencyPosition currencyPosition = new CurrencyPosition();
            currencyPosition.setQuantity(config.getInitialCash());
            currencyPosition.setOpened(new Date());
            currencyPosition.setBaseCurrency(currency);

            currency.getCurrencyPositions().add(currencyPosition);
            currency.setPortfolio(portfolio);

            security = getSecurity(portfolio);
            security.setPortfolio(portfolio);
            security.setCurrency(currency);

            portfolio.getCurrencies().add(currency);
            portfolio.getSecurities().add(security);
            Database.persistPortfolio(portfolio);
        } else {
            security = getSecurity(portfolio);
        }

        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = intradayPrices.get(0);
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        Calc c = new Calc(security, config, price);
        for (int i = 0; i < intradayPrices.size(); i++) {
            price = intradayPrices.get(i);
            shortMAValue = Util.getMA(intradayPrices, previousShortMAValue, i, config.getShortLookback(), price);
            longMAValue = Util.getMA(intradayPrices, previousLongMAValue, i, config.getLongLookback(), price);
            c.updateCalc(price, shortMAValue, longMAValue, portfolio);
            c.decide();

            LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,000000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), Util.getPortfolioValue(portfolio, security.getCurrency(), price), shortMAValue, longMAValue, price, i));
            previousShortMAValue = shortMAValue;
            previousLongMAValue = longMAValue;
        }

        Database.persistPortfolio(portfolio);

        String msg = MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), Util.getPortfolioValue(portfolio, security.getCurrency(), price));
        LOG.debug(msg);

        security.getSecurityPositions().forEach(ps -> LOG.info(ps.getPrice() + ", " + ps.getQuantity() + " : " + ps.getPrice().multiply(ps.getQuantity())));
    }

    private static Security getSecurity(Portfolio portfolio) {
        Security security = new Security();
        security.setSymbol("AAPL");
        return portfolio.getSecurities().stream().findFirst().orElse(security);
    }
}
