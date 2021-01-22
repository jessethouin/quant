package com.jessethouin.quant.backtest;

import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.broker.TimedExecutor;
import com.jessethouin.quant.conf.BuyStrategyTypes;
import com.jessethouin.quant.conf.SellStrategyTypes;
import com.jessethouin.quant.db.Database;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BacktestParameterCombos extends AbstractBacktest {
    public static final ConcurrentLinkedQueue<BacktestParameterResults> BACKTEST_RESULTS_QUEUE = new ConcurrentLinkedQueue<>();
    public static final ConcurrentLinkedQueue<ProcessHistoricIntradayPrices> BACKTEST_QUEUE = new ConcurrentLinkedQueue<>();
    private static final TimedExecutor PROCESS_EXECUTOR_SERVICE = TimedExecutor.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private static final Logger LOG = LogManager.getLogger(BacktestParameterCombos.class);
    private static String best = "";
    private static BigDecimal bestv = BigDecimal.ZERO;

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
        getAllowanceCombos(minMALookback, maxMALookback, riskMax, riskIncrement);

        LOG.info(MessageFormat.format("{0} combinations to be tested.", BACKTEST_QUEUE.size()));

        BigDecimal initialBacktestQueueSize = BigDecimal.valueOf(BACKTEST_QUEUE.size());
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

        timer.scheduleAtFixedRate(() -> {
            BigDecimal currentBacktestQueueSize = initialBacktestQueueSize.subtract(BigDecimal.valueOf(PROCESS_EXECUTOR_SERVICE.getCompletedTaskCount()));
            System.out.print(MessageFormat.format("{0,number,percent} complete. {1} remaining.\r", currentBacktestQueueSize.divide(initialBacktestQueueSize, 4, RoundingMode.HALF_UP), getRemainingTimeUnits(currentBacktestQueueSize)));
        }, 0, 1, TimeUnit.SECONDS);

        while (BACKTEST_QUEUE.peek() != null) {
            PROCESS_EXECUTOR_SERVICE.execute(BACKTEST_QUEUE.poll());
        }


        try {
            PROCESS_EXECUTOR_SERVICE.shutdown();
            boolean executed = PROCESS_EXECUTOR_SERVICE.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!executed) LOG.warn("Well, we waited 292 years and it still didn't finish. Probably a leak somewhere.");
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage());
        }

        persistComboResults();

        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        logMarketChange(intradayPrices.get(intradayPrices.size() - 1), intradayPrices.get(0), LOG);

        watch.stop();
        Duration elapsedTime = Duration.ofMillis(watch.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Time Elapsed: {}", String.format("%02d:%02d:%02d", elapsedTime.toHours(), elapsedTime.toMinutesPart(), elapsedTime.toSecondsPart()));
    }

    private static void getAllowanceCombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement) {
        if (CONFIG.isBacktestAllowance()) {
            for (BigDecimal i = BigDecimal.valueOf(.1); i.compareTo(BigDecimal.ONE) <= 0; i = i.add(BigDecimal.valueOf(.1))) {
                getBuySellCombos(minMALookback, maxMALookback, riskMax, riskIncrement, i);
            }
        } else {
            getBuySellCombos(minMALookback, maxMALookback, riskMax, riskIncrement, CONFIG.getAllowance());
        }
    }

    private static void getBuySellCombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement, BigDecimal allowance) {
        if (CONFIG.isBacktestStrategy()) {
            Arrays.stream(BuyStrategyTypes.values()).forEach(buyStrategyType -> Arrays.stream(SellStrategyTypes.values()).forEach(sellStrategyType -> {
                        try {
                            getMACombos(buyStrategyType, sellStrategyType, minMALookback, maxMALookback, riskMax, riskIncrement, allowance);
                        } catch (InterruptedException e) {
                            LOG.error(e.getMessage());
                        }
                    }
            ));
        } else {
            try {
                getMACombos(CONFIG.getBuyStrategy(), CONFIG.getSellStrategy(), minMALookback, maxMALookback, riskMax, riskIncrement, allowance);
            } catch (InterruptedException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    private static void getMACombos(BuyStrategyTypes buyStrategyType, SellStrategyTypes sellStrategyType, int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement, BigDecimal allowance) throws InterruptedException {
        int shortLookback;
        int longLookback = 0;
        while (++longLookback <= maxMALookback) {
            shortLookback = minMALookback;
            while (++shortLookback < longLookback) { // there's no need to test equal short and long tail MAs because they will never separate or converge. That's why this is < and not <=.
                getRiskCombos(buyStrategyType, sellStrategyType, riskMax, riskIncrement, shortLookback, longLookback, allowance);
            }
        }
    }

    private static void getRiskCombos(BuyStrategyTypes buyStrategyType, SellStrategyTypes sellStrategyType, BigDecimal riskMax, BigDecimal riskIncrement, int shortLookback, int longLookback, BigDecimal allowance) {
        BigDecimal lowRisk;
        BigDecimal highRisk = BigDecimal.ZERO;
        while (highRisk.compareTo(riskMax) < 0) {
            highRisk = highRisk.add(riskIncrement);
            lowRisk = BigDecimal.ZERO;
            while (lowRisk.compareTo(riskMax) < 0) {
                lowRisk = lowRisk.add(riskIncrement);
                BACKTEST_QUEUE.offer(new ProcessHistoricIntradayPrices(buyStrategyType, sellStrategyType, shortLookback, longLookback, highRisk, lowRisk, allowance, intradayPrices));
            }
        }
    }

    private static void persistComboResults() {
        LOG.trace("Writing results to the database. Please wait.");
        BacktestParameterResults r;
        Session session = Database.getSession();
        session.beginTransaction();
        while (BACKTEST_RESULTS_QUEUE.peek() != null) {
            r = BACKTEST_RESULTS_QUEUE.poll();
            session.persist(r);
        }
        session.getTransaction().commit();
    }

    private static String getRemainingTimeUnits(BigDecimal currentBacktestQueueSize) {
        BigDecimal avgProcessingTime = BigDecimal.valueOf(PROCESS_EXECUTOR_SERVICE.getMedianExecutionTime());
        BigDecimal remainingTime = avgProcessingTime.multiply(currentBacktestQueueSize);

        Duration timeLeft = Duration.ofMillis(remainingTime.longValue());
        return String.format("%02d:%02d:%02d", timeLeft.toHours(), timeLeft.toMinutesPart(), timeLeft.toSecondsPart());
    }

    private static BigDecimal getBestv() {
        return bestv;
    }

    private static synchronized void setBestv(BigDecimal bestv) {
        BacktestParameterCombos.bestv = bestv;
        LOG.info(MessageFormat.format("\n\nHomer Simpson: The best combination of parameters SO FAR is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));
    }

    private static String getBest() {
        return best;
    }

    private static synchronized void setBest(String best) {
        BacktestParameterCombos.best = best;
    }

    public static synchronized void updateBest(String msg, BigDecimal v) {
        if (v.compareTo(getBestv()) > 0) {
            setBest(msg);
            setBestv(v);
        }
    }
}
