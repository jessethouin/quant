package com.jessethouin.quant.backtest;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BacktestParameterCombos extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestParameterCombos.class);
    static String best = "";
    static BigDecimal bestv = BigDecimal.ZERO;

    public static void findBestCombos(String[] args) throws InterruptedException {
        StopWatch watch = new StopWatch();
        watch.start();

        int minMALookback = 0;
        int maxMALookback = 10;
        BigDecimal riskMax = BigDecimal.valueOf(.10);
        BigDecimal riskIncrement = BigDecimal.valueOf(.01);

        if (args.length != 0 && args.length != 4) {
            LOG.error("Listen. You either need to supply 4 arguments or none. Stop trying to half-ass this thing. minMALookback, maxMALookback, riskMax, and riskIncrement if you need specifics. Otherwise, shut up and let me do the work.");
            return;
        }

        if (args.length == 0) LOG.info(MessageFormat.format("Using default values for minMALookback {0}, maxMALookback {1}, riskMax {2}, and riskIncrement {3}.", minMALookback, maxMALookback, riskMax, riskIncrement));

        for (int i = 0; i < args.length; i++) {
            switch (i) {
                case 0 -> minMALookback = Integer.parseInt(args[i]);
                case 1 -> maxMALookback = Integer.parseInt(args[i]);
                case 2 -> riskMax = BigDecimal.valueOf(Double.parseDouble(args[i]));
                case 3 -> riskIncrement = BigDecimal.valueOf(Double.parseDouble(args[i]));
            }
        }

        if (minMALookback > maxMALookback) {
            LOG.error(MessageFormat.format("Starting moving average count {0} must be less than maxMALookback {1}", minMALookback, maxMALookback));
            return;
        }

        populateIntradayPrices();

        LOG.info(MessageFormat.format("{0} combinations were tested.", getMACombos(minMALookback, maxMALookback, riskMax, riskIncrement)));
        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        watch.stop();
        LOG.info(MessageFormat.format("Time Elapsed: {0}", watch.getTime(TimeUnit.MILLISECONDS) / 1000));
    }

    private static int getMACombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement) throws InterruptedException {
        int shortLookback;
        int longLookback = 0, c = 0, combos = 0;
        int lookbackRange = maxMALookback - minMALookback;
        int i = ((lookbackRange * (lookbackRange + 1)) / 2) - lookbackRange;
        int i_ = i;
        List<Long> nt_ = new ArrayList<>();
        StopWatch loopWatch = new StopWatch();

        while (++longLookback <= maxMALookback) {
            shortLookback = minMALookback;
            while (++shortLookback < longLookback) { // there's no need to test equal short and long tail MAs because they will never separate or converge. That's why this is < and not <=.
                combos = getRiskCombos(riskMax, riskIncrement, shortLookback, longLookback, combos, nt_, loopWatch);

                double[] timeUntis = getRemainingTimeUnits(nt_, --i_);
                LOG.info(MessageFormat.format("{0,number,percent} complete. {1,number,00:}{2,number,00:}{3,number,00.00} remaining", ++c / (float) i, timeUntis[0], timeUntis[1], timeUntis[2]));
            }
        }
        return combos;
    }

    private static int getRiskCombos(BigDecimal riskMax, BigDecimal riskIncrement, int shortLookback, int longLookback, int combos, List<Long> nt_, StopWatch loopWatch) throws InterruptedException {
        long nt;
        BigDecimal lowRisk;
        BigDecimal highRisk = BigDecimal.ZERO;
        loopWatch.start();
        while (highRisk.compareTo(riskMax) < 0) {
            highRisk = highRisk.add(riskIncrement);
            lowRisk = BigDecimal.ZERO;
            ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Callable<Object>> todo = new ArrayList<>();
            while (lowRisk.compareTo(riskMax) < 0) {
                combos++;
                lowRisk = lowRisk.add(riskIncrement);
                todo.add(new ProcessHistoricIntradayPrices(shortLookback, longLookback, highRisk, lowRisk, intradayPrices));
            }
            es.invokeAll(todo);
            es.shutdown();
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        loopWatch.stop();
        nt = loopWatch.getTime(TimeUnit.MILLISECONDS);
        nt_.add(nt);
        loopWatch.reset();
        return combos;
    }

    private static double[] getRemainingTimeUnits(List<Long> nt_, int i_) {
        double r = ((nt_.stream().mapToLong(a -> a).average().orElse(0)) / (double) 1000) * i_; // average numer of seconds it takes to work though all the possible high/low risk combinations.
        double[] timeUntis = {0,0,0};
        r = r % (24 * 3600);
        timeUntis[0] = r / 3600;
        r %= 3600;
        timeUntis[1] = r / 60 ;
        r %= 60;
        timeUntis[2] = r;
        return timeUntis;
    }

    public static BigDecimal getBestv() {
        return bestv;
    }

    public static synchronized void setBestv(BigDecimal bestv) {
        BacktestParameterCombos.bestv = bestv;
        LOG.info(MessageFormat.format("\n\nHomer Simpson: The best combination of parameters SO FAR is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));
    }

    public static String getBest() {
        return best;
    }

    public static synchronized void setBest(String best) {
        BacktestParameterCombos.best = best;
    }

    public static synchronized void updateBest(String msg, BigDecimal v) {
        if (v.compareTo(getBestv()) > 0) {
            setBest(msg);
            setBestv(v);
        }
    }
}