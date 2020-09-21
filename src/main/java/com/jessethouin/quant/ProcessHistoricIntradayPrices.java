package com.jessethouin.quant;

import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.calculators.MATypes;
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
    int finalS;
    int finalL;
    BigDecimal finalRh;
    BigDecimal finalRl;
    List<BigDecimal> intradayPrices;

    public ProcessHistoricIntradayPrices(int finalS, int finalL, BigDecimal finalRh, BigDecimal finalRl, List<BigDecimal> intradayPrices) {
        this.finalS = finalS;
        this.finalL = finalL;
        this.finalRh = finalRh;
        this.finalRl = finalRl;
        this.intradayPrices = intradayPrices;
    }

    @Override
    public BigDecimal call() {

        Config config = new Config(finalS, finalL, finalRh, finalRl);
        Portfolio portfolio = new Portfolio();
        portfolio.setCash(config.getInitialCash());
        Security aapl = new Security("AAPL");
        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(aapl)));

        Calc c = new Calc(aapl, config, intradayPrices.get(0));
        BigDecimal s;
        BigDecimal l;
        BigDecimal price;
        BigDecimal previous = BigDecimal.ZERO;
        for (int i = 0; i < intradayPrices.size(); i++) {
            price = intradayPrices.get(i);
            s = getMA(previous, i, config.getShortPeriod());
            l = getMA(previous, i, config.getLongPeriod());
            c.updateCalc(price, s, l, portfolio);
            try {
                portfolio.addCash(c.decide());
            } catch (CashException e) {
                LOG.error(e);
            }

//            LOG.debug(MessageFormat.format("{8,number,000} : {0,number,00} {5,number,000.000} : {1,number,00} {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", finalS, finalL, finalRl, finalRh, portfolio.getPortfolioValue(), s, l, price, i));
            previous = price;
        }

        LOG.debug(MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", finalS, finalL, finalRl, finalRh, portfolio.getPortfolioValue()));
        return portfolio.getPortfolioValue();
/*
        if (portfolio.getPortfolioValue().compareTo(Main.getBestv()) > 0) {
            Main.setBest("short: " + config.getShortPeriod() + " long: " + config.getLongPeriod() + " rl: " + config.getLowRisk().toPlainString() + " rh: " + config.getHighRisk().toPlainString() + " v: " + portfolio.getPortfolioValue());
            Main.setBestv(portfolio.getPortfolioValue());
        }
*/
    }

    private BigDecimal getMA(BigDecimal previous, int i, int p) {
        BigDecimal ma;
        if (i < p) ma = BigDecimal.ZERO;
        else if (i == p) ma = SMA.sma(intradayPrices.subList(0, i), p);
        else ma = MA.ma(intradayPrices.get(i), previous, p, MATypes.DEMA);
        return ma;
    }
}
