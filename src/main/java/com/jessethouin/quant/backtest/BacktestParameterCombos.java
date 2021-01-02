package com.jessethouin.quant.backtest;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Duration;
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
        Duration elapsedTime = Duration.ofMillis(watch.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Time Elapsed: {}", String.format("%02d:%02d:%02d", elapsedTime.toHours(), elapsedTime.toMinutesPart(), elapsedTime.toSecondsPart()));
    }

    private static int getMACombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement) throws InterruptedException {
        int shortLookback;
        int longLookback = 0, c = 0, combos = 0;
        int lookbackRange = maxMALookback - minMALookback;
        int i = ((lookbackRange * (lookbackRange + 1)) / 2) - lookbackRange;
        int i_ = i;
        List<Long> comboProcessingTimes = new ArrayList<>();
        StopWatch loopWatch = new StopWatch();

        while (++longLookback <= maxMALookback) {
            shortLookback = minMALookback;
            while (++shortLookback < longLookback) { // there's no need to test equal short and long tail MAs because they will never separate or converge. That's why this is < and not <=.
                combos = getRiskCombos(riskMax, riskIncrement, shortLookback, longLookback, combos, comboProcessingTimes, loopWatch);

                LOG.info(MessageFormat.format("{0,number,percent} complete. {1} remaining ({2}, {3})", ++c / (float) i, getRemainingTimeUnits(comboProcessingTimes, --i_), shortLookback, longLookback));
            }
        }
        return combos;
    }

    private static int getRiskCombos(BigDecimal riskMax, BigDecimal riskIncrement, int shortLookback, int longLookback, int combos, List<Long> comboProcessingTimes, StopWatch loopWatch) throws InterruptedException {
        long comboProcessingTime;
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
        comboProcessingTime = loopWatch.getTime(TimeUnit.MILLISECONDS);
        comboProcessingTimes.add(comboProcessingTime);
        loopWatch.reset();
        return combos;
    }

    private static String getRemainingTimeUnits(List<Long> comboProcessingTimes, int i_) {
        BigDecimal avgProcessingTimes = BigDecimal.valueOf((comboProcessingTimes.stream().mapToLong(a -> a).average().orElse(0)));
        BigDecimal r = avgProcessingTimes.multiply(BigDecimal.valueOf(i_));
        Duration timeLeft = Duration.ofMillis(r.longValue());
        return String.format("%02d:%02d:%02d", timeLeft.toHours(), timeLeft.toMinutesPart(), timeLeft.toSecondsPart());
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
