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
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.*;

public class BacktestParameterCombos extends AbstractBacktest {
    public static final ConcurrentLinkedQueue<BacktestParameterResults> BACKTEST_RESULTS_QUEUE = new ConcurrentLinkedQueue<>();
    private static final ThreadPoolExecutor PROCESS_EXECUTOR_SERVICE = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    private static final Logger LOG = LogManager.getLogger(BacktestParameterCombos.class);
    private static String best = "";
    private static BigDecimal bestv = BigDecimal.ZERO;

    static int count = 0;

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

        LOG.info(MessageFormat.format("{0} combinations to be tested.", count));

        BigDecimal initialBacktestQueueSize = BigDecimal.valueOf(count);
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

        timer.scheduleAtFixedRate(() -> {
            BacktestParameterResults backtestParameterResults = BACKTEST_RESULTS_QUEUE.stream().max(Comparator.comparing(BacktestParameterResults::getTimestamp)).orElse(null);
            if (backtestParameterResults == null) return;
            int currentLong = backtestParameterResults.getLongLookback();
            int currentShort = backtestParameterResults.getShortLookback();
            BigDecimal currentBacktestQueueSize = initialBacktestQueueSize.subtract(BigDecimal.valueOf(PROCESS_EXECUTOR_SERVICE.getCompletedTaskCount()));
            BigDecimal percentRemaining = currentBacktestQueueSize.divide(initialBacktestQueueSize, 8, RoundingMode.HALF_UP);
            System.out.print(MessageFormat.format("{2}/{3} {0,number,percent} {1} remaining. ({4}, {5})\r", percentRemaining, getRemainingTimeUnits(BigDecimal.valueOf(count), percentRemaining, BigDecimal.valueOf(watch.getTime(TimeUnit.MILLISECONDS))), currentBacktestQueueSize, initialBacktestQueueSize, currentShort, currentLong));
        }, 0, 2, TimeUnit.SECONDS);

        try {
            PROCESS_EXECUTOR_SERVICE.shutdown();
            boolean executed = PROCESS_EXECUTOR_SERVICE.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!executed) LOG.warn("Well, we waited 292 years and it still didn't finish. Probably a leak somewhere.");
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage());
        }

        timer.shutdown();
        persistComboResults();

        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        logMarketChange(INTRADAY_PRICES.get(INTRADAY_PRICES.size() - 1), INTRADAY_PRICES.get(0), LOG);

        watch.stop();
        Duration elapsedTime = Duration.ofMillis(watch.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Time Elapsed: {}", String.format("%02d:%02d:%02d", elapsedTime.toHours(), elapsedTime.toMinutesPart(), elapsedTime.toSecondsPart()));
    }

    private static void getAllowanceCombos(int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement) {
        if (CONFIG.isBacktestAllowance()) {
            for (BigDecimal i = riskIncrement; i.compareTo(riskMax) <= 0; i = i.add(riskIncrement)) {
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
        int longLookback = minMALookback;
        while (longLookback <= maxMALookback) {
//            shortLookback = minMALookback;
            shortLookback = longLookback - 10; //having a difference of more than 10 has not proven to be profitable
            while (shortLookback < longLookback) { // there's no need to test equal short and long tail MAs because they will never separate or converge. That's why this is < and not <=.
                getRiskCombos(buyStrategyType, sellStrategyType, riskMax, riskIncrement, shortLookback, longLookback, allowance);
                shortLookback++;
            }
            longLookback++;
        }
    }

    private static void getRiskCombos(BuyStrategyTypes buyStrategyType, SellStrategyTypes sellStrategyType, BigDecimal riskMax, BigDecimal riskIncrement, int shortLookback, int longLookback, BigDecimal allowance) {
        BigDecimal lowRisk;
        BigDecimal highRisk = BigDecimal.ZERO;
        while (highRisk.compareTo(riskMax) < 0) {
            lowRisk = BigDecimal.ZERO;
            while (lowRisk.compareTo(riskMax) < 0) {
                count++;
                PROCESS_EXECUTOR_SERVICE.execute(new ProcessHistoricIntradayPrices(buyStrategyType, sellStrategyType, shortLookback, longLookback, highRisk, lowRisk, allowance));
                lowRisk = lowRisk.add(riskIncrement);
            }
            highRisk = highRisk.add(riskIncrement);
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

    private static String getRemainingTimeUnits(BigDecimal t, BigDecimal r, BigDecimal c) {
        if  (r.compareTo(BigDecimal.ONE) >= 0) return "Calculating remaining time...";
        BigDecimal remainingTime = (t.multiply(r)).multiply((c).divide((BigDecimal.ONE.subtract(r)).multiply(t), 8, RoundingMode.HALF_UP));
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
