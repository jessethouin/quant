package com.jessethouin.quant.backtest;

import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.conf.BuyStrategyTypes;
import com.jessethouin.quant.conf.SellStrategyTypes;
import com.jessethouin.quant.db.Database;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BacktestParameterCombos extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestParameterCombos.class);
    public static final ConcurrentLinkedQueue<BacktestParameterResults> QUEUE = new ConcurrentLinkedQueue<>();
    static String best = "";
    static BigDecimal bestv = BigDecimal.ZERO;

    public static void findBestCombos(String[] args) {
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

        if (args.length == 0)
            LOG.info(MessageFormat.format("Using default values for minMALookback {0}, maxMALookback {1}, riskMax {2}, and riskIncrement {3}.", minMALookback, maxMALookback, riskMax, riskIncrement));

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

        LOG.info(MessageFormat.format("{0} combinations were tested.", getAllowanceCombos(minMALookback, maxMALookback, riskMax, riskIncrement)));
        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        persistComboResults();

        watch.stop();
        Duration elapsedTime = Duration.ofMillis(watch.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Time Elapsed: {}", String.format("%02d:%02d:%02d", elapsedTime.toHours(), elapsedTime.toMinutesPart(), elapsedTime.toSecondsPart()));
    }

    private static int getAllowanceCombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement) {
        if (CONFIG.isBacktestAllowance()) {
            int allowanceCombos = 0;
            for (double i = .1; i <= 1; i += .1) {
                allowanceCombos += getBuySellCombos(minMALookback, maxMALookback, riskMax, riskIncrement, BigDecimal.valueOf(i));
            }
            return allowanceCombos;
        } else {
            return getBuySellCombos(minMALookback, maxMALookback, riskMax, riskIncrement, CONFIG.getAllowance());
        }
    }

    private static int getBuySellCombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement, BigDecimal allowance) {
        if (CONFIG.isBacktestStrategy()) {
            AtomicInteger buySellCombos = new AtomicInteger();
            Arrays.stream(BuyStrategyTypes.values()).forEach(buyStrategyType -> Arrays.stream(SellStrategyTypes.values()).forEach(sellStrategyType -> {
                        try {
                            buySellCombos.set(getMACombos(buyStrategyType, sellStrategyType, minMALookback, maxMALookback, riskMax, riskIncrement, allowance));
                        } catch (InterruptedException e) {
                            LOG.error(e.getMessage());
                        }
                    }
            ));
            return buySellCombos.get() * BuyStrategyTypes.values().length * SellStrategyTypes.values().length;
        } else {
            try {
                return getMACombos(CONFIG.getBuyStrategy(), CONFIG.getSellStrategy(), minMALookback, maxMALookback, riskMax, riskIncrement, allowance);
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
            }
        }
        return 0;
    }

    private static int getMACombos(BuyStrategyTypes buyStrategyType, SellStrategyTypes sellStrategyType, int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement, BigDecimal allowance) throws InterruptedException {
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
                combos = getRiskCombos(buyStrategyType, sellStrategyType, riskMax, riskIncrement, shortLookback, longLookback, combos, comboProcessingTimes, loopWatch, allowance);

                LOG.info(MessageFormat.format("{0,number,percent} complete. {1} remaining ({2}, {3}, {6}, {4}/{5})", ++c / (float) i, getRemainingTimeUnits(comboProcessingTimes, --i_), shortLookback, longLookback, buyStrategyType, sellStrategyType, allowance));
            }
        }
        return combos;
    }

    private static int getRiskCombos(BuyStrategyTypes buyStrategyType, SellStrategyTypes sellStrategyType, BigDecimal riskMax, BigDecimal riskIncrement, int shortLookback, int longLookback, int combos, List<Long> comboProcessingTimes, StopWatch loopWatch, BigDecimal allowance) throws InterruptedException {
        long comboProcessingTime;
        BigDecimal lowRisk;
        BigDecimal highRisk = BigDecimal.ZERO;
        loopWatch.start();
        while (highRisk.compareTo(riskMax) < 0) {
            highRisk = highRisk.add(riskIncrement);
            lowRisk = BigDecimal.ZERO;
            ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            while (lowRisk.compareTo(riskMax) < 0) {
                combos++;
                lowRisk = lowRisk.add(riskIncrement);
                es.execute(new ProcessHistoricIntradayPrices(buyStrategyType, sellStrategyType, shortLookback, longLookback, highRisk, lowRisk, allowance, intradayPrices));
            }
            es.shutdown();
            boolean executed = es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!executed) LOG.warn("Well, we waited 292 years and it still didn't finish. Probably a leak somewhere.");
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

    private static void persistComboResults() {
        LOG.info("Writing results to the database. Please wait.");
        BacktestParameterResults r;
        Session session = Database.getSession();
        session.beginTransaction();
        while (QUEUE.peek() != null) {
            r = QUEUE.poll();
            session.persist(r);
        }
        session.getTransaction().commit();
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
