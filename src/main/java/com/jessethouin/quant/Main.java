package com.jessethouin.quant;

import com.jessethouin.quant.calculators.*;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.exceptions.CashException;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws CashException {
        StopWatch watch = new StopWatch();
        watch.start();

        Portfolio portfolio = new Portfolio();
        portfolio.setCash(Config.getInitialCash());

        Security aapl = new Security("AAPL");
//        Security goog = new Security("GOOG"); // TODO: ability to work with multiple securities

        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(aapl)));

        List<BigDecimal> intradayPrices = new ArrayList<>();

        InputStream aapl_csv = Thread.currentThread().getContextClassLoader().getResourceAsStream("AAPL.csv");
        if (aapl_csv == null) throw new NullPointerException("aapl_csv was null for some reason. Perhaps the file didn't exist, or we didn't have permissions to read it.");
        try (Scanner scanner = new Scanner(aapl_csv)) {
            while (scanner.hasNextLine()) {
                try (Scanner rowScanner = new Scanner(scanner.nextLine())) {
                    rowScanner.useDelimiter(",");
                    while (rowScanner.hasNext()) {
                        rowScanner.next();
                        intradayPrices.add(rowScanner.nextBigDecimal());
                    }
                }
            }
        }


        BigDecimal rh;
        BigDecimal rl;
        BigDecimal rmax = BigDecimal.valueOf(.25);

        String best = "";
        BigDecimal bestv = BigDecimal.ZERO;
        StopWatch loopWatch = new StopWatch();

        int s;
        int l = 0;
        int max = 200;
        while (l < max) {
            loopWatch.start();
            Config.setLongPeriod(l++);
            s = 1;
            while (s < l) {
                Config.setShortPeriod(s++);
                rh = BigDecimal.ZERO;
                while (rh.compareTo(rmax) < 0) {
                    rh = rh.add(BigDecimal.valueOf(.01));
                    Config.setHighRisk(rh);
                    rl = BigDecimal.ZERO;
                    while (rl.compareTo(rmax) < 0) {
                        rl = rl.add(BigDecimal.valueOf(.01));
                        Config.setLowRisk(rl);

                        portfolio = new Portfolio();
                        portfolio.setCash(Config.getInitialCash());
                        aapl = new Security("AAPL");
                        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(aapl)));

                        Calc c = new Calc(aapl, intradayPrices.get(0));

                        List<BigDecimal> shortOutput = MA.ma(intradayPrices, Config.getShortPeriod(), MATypes.DEMA);
                        List<BigDecimal> longOutput = MA.ma(intradayPrices, Config.getLongPeriod(), MATypes.DEMA);

                        for (int i = 0; i < intradayPrices.size(); i++) {
                            LOG.debug(i + ": " + shortOutput.get(i) + " : " + longOutput.get(i) + " : " + intradayPrices.get(i) + " : " + portfolio.getCash());
                            c.updateCalc(intradayPrices.get(i), shortOutput.get(i), longOutput.get(i), portfolio);
                            portfolio.addCash(c.decide());
                        }

                        LOG.debug(portfolio.toString());

                        if (portfolio.getPortfolioValue().compareTo(bestv) > 0) {
                            best = "short: " + s + " long: " + l + " rl: " + rl.toPlainString() + " rh: " + rh.toPlainString() + " v: " + portfolio.getPortfolioValue();
                            bestv = portfolio.getPortfolioValue();
                        }
                        LOG.debug("short: " + s + " long: " + l + " v: " + portfolio.getPortfolioValue());
                    }
                }
            }
            loopWatch.stop();
            LOG.info(l + " - " + loopWatch.getTime(TimeUnit.MILLISECONDS));
            loopWatch.reset();
        }
        LOG.info(best);
        watch.stop();
        LOG.info("Time Elapsed: " + watch.getTime(TimeUnit.MILLISECONDS));
//        ImageCharts line = new ImageCharts().cht("ls").chd()
    }
}
