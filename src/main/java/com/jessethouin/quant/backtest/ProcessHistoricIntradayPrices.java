package com.jessethouin.quant.backtest;

import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.conf.MATypes;
import com.jessethouin.quant.calculators.SMA;
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
    final int s;
    final int l;
    final BigDecimal rh;
    final BigDecimal rl;
    final List<BigDecimal> intradayPrices;

    public ProcessHistoricIntradayPrices(int s, int l, BigDecimal rh, BigDecimal rl, List<BigDecimal> intradayPrices) {
        this.s = s;
        this.l = l;
        this.rh = rh;
        this.rl = rl;
        this.intradayPrices = intradayPrices;
    }

    @Override
    public BigDecimal call() {
        Config config = new Config();
        config.setLowRisk(rl);
        config.setHighRisk(rh);

        Portfolio portfolio = new Portfolio();
        portfolio.setCash(config.getInitialCash());

        Security security = new Security();
        security.setSymbol("AAPL");
        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(security)));

        Calc c = new Calc(security, config, intradayPrices.get(0));
        BigDecimal sv;
        BigDecimal lv;
        BigDecimal price = intradayPrices.get(0);
        BigDecimal previous = BigDecimal.ZERO;
        for (int i = 0; i < intradayPrices.size(); i++) {
            price = intradayPrices.get(i);
            sv = getMA(previous, i, s);
            lv = getMA(previous, i, l);
            c.updateCalc(price, sv, lv, portfolio);
            try {
                Transactions.addCash(portfolio, c.decide());
            } catch (CashException e) {
                LOG.error(e);
            }

            LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", s, l, rl, rh, Transactions.getPortfolioValue(portfolio, security.getSymbol(), price), sv, lv, price, i));
            previous = price;
        }

        BigDecimal pValue = Transactions.getPortfolioValue(portfolio, security.getSymbol(), price);
        String msg = MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", s, l, rl, rh, pValue);
        LOG.debug(msg);
        Backtest.updateBest(msg, pValue);

        return pValue;
    }

    private BigDecimal getMA(BigDecimal previous, int i, int p) {
        BigDecimal ma;
        if (i < p) ma = BigDecimal.ZERO;
        else if (i == p) ma = SMA.sma(intradayPrices.subList(0, i), p);
        else ma = MA.ma(intradayPrices.get(i), previous, p, MATypes.TEMA);
        return ma;
    }
}
