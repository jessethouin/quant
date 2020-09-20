package com.jessethouin.quant;

import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.calculators.MATypes;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.exceptions.CashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProcessHistoricIntradayPrices implements Runnable {
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
    public void run() {
        Config config = new Config(finalS, finalL, finalRh, finalRl);
        Portfolio portfolio = new Portfolio();
        portfolio.setCash(config.getInitialCash());
        Security aapl = new Security("AAPL");
        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(aapl)));

        Calc c = new Calc(aapl, config, intradayPrices.get(0));

        List<BigDecimal> shortOutput = MA.ma(intradayPrices, config.getShortPeriod(), MATypes.DEMA);
        List<BigDecimal> longOutput = MA.ma(intradayPrices, config.getLongPeriod(), MATypes.DEMA);

        for (int i = 0; i < intradayPrices.size(); i++) {
            LOG.trace(MessageFormat.format("{0}: rl {7} : rh {8} : s {1} {2} : l {3} {4} : p {5} : v {6}", i, finalS, shortOutput.get(i), finalL, longOutput.get(i), intradayPrices.get(i), portfolio.getCash(), finalRl, finalRh));

            c.updateCalc(intradayPrices.get(i), shortOutput.get(i), longOutput.get(i), portfolio);
            try {
                portfolio.addCash(c.decide());
            } catch (CashException e) {
                LOG.error(e);
            }
        }

        LOG.debug(portfolio.toString());

        if (portfolio.getPortfolioValue().compareTo(Main.getBestv()) > 0) {
            Main.setBest("short: " + config.getShortPeriod() + " long: " + config.getLongPeriod() + " rl: " + config.getLowRisk().toPlainString() + " rh: " + config.getHighRisk().toPlainString() + " v: " + portfolio.getPortfolioValue());
            Main.setBestv(portfolio.getPortfolioValue());
        }
        LOG.debug("short: " + config.getShortPeriod() + " long: " + config.getLongPeriod() + " rl: " + config.getLowRisk().toPlainString() + " rh: " + config.getHighRisk().toPlainString() + " v: " + portfolio.getPortfolioValue());
    }
}
