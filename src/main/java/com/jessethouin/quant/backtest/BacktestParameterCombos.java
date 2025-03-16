package com.jessethouin.quant.backtest;

import com.jessethouin.quant.alpaca.AlpacaCaptureHistory;
import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.backtest.beans.repos.BacktestParameterResultsRepository;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.conf.BuyStrategyType;
import com.jessethouin.quant.conf.SellStrategyType;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

import static com.jessethouin.quant.conf.Config.CONFIG;

@Component
public class BacktestParameterCombos extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestParameterCombos.class);

    public static final ConcurrentLinkedQueue<BacktestParameterResults> BACKTEST_RESULTS_QUEUE = new ConcurrentLinkedQueue<>();
    private static ThreadPoolExecutor processExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    private static BacktestParameterResults bestv = BacktestParameterResults.builder().value(BigDecimal.ZERO).build();
    private static BacktestParameterResults lastBestv = bestv;

    static int count = 0;
    static boolean save = true;

    public BacktestParameterCombos(TradeHistoryRepository tradeHistoryRepository, BacktestParameterResultsRepository backtestParameterResultsRepository, BinanceCaptureHistory binanceCaptureHistory, AlpacaCaptureHistory alpacaCaptureHistory) {
        super(tradeHistoryRepository, backtestParameterResultsRepository, binanceCaptureHistory, alpacaCaptureHistory);
    }

    public BacktestParameterResults findBestCombo() {
        save = false;
        findBestCombos(new String[]{"5", "500", "1.00", "0.10"});
        return bestv;
    }

    public void findBestCombos(String[] args) {
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

        count = 0;
        bestv = BacktestParameterResults.builder().value(BigDecimal.ZERO).build();
        lastBestv = bestv;
        processExecutorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        BACKTEST_RESULTS_QUEUE.clear();
        populateIntradayPrices();

        getAllowanceCombos(minMALookback, maxMALookback, riskMax, riskIncrement); // and that, kids, is how I met your mother; this is where the magic happens; give a little slappy, make daddy happy

        LOG.info(MessageFormat.format("{0} combinations to be tested.", count));

        BigDecimal initialBacktestQueueSize = BigDecimal.valueOf(count);
        ScheduledExecutorService statusScheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        logStatus(watch, initialBacktestQueueSize, statusScheduledExecutorService);

        try {
            processExecutorService.shutdown();
            boolean executed = processExecutorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!executed)
                LOG.warn("Well, we waited 292 years and it still didn't finish. Probably a leak somewhere.");
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage());
        }

        statusScheduledExecutorService.shutdown();
        bestv = BACKTEST_RESULTS_QUEUE.stream().max(Comparator.comparing(BacktestParameterResults::getValue)).orElse(BacktestParameterResults.builder().value(BigDecimal.ZERO).build());
        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", bestv, bestv.getValue()));

        persistComboResults();
        logMarketChange(INTRADAY_PRICES.getLast(), INTRADAY_PRICES.getFirst(), LOG);

        watch.stop();
        Duration elapsedTime = Duration.ofMillis(watch.getTime(TimeUnit.MILLISECONDS));
        LOG.info("Time Elapsed: {}", String.format("%02d:%02d:%02d", elapsedTime.toHours(), elapsedTime.toMinutesPart(), elapsedTime.toSecondsPart()));

        if (save) System.exit(0);
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
            Arrays.stream(BuyStrategyType.values()).forEach(
                    buyStrategyType -> Arrays.stream(SellStrategyType.values()).forEach(
                            sellStrategyType -> getMACombos(buyStrategyType, sellStrategyType, minMALookback, maxMALookback, riskMax, riskIncrement, allowance)
                    )
            );
        } else {
            getMACombos(CONFIG.getBuyStrategy(), CONFIG.getSellStrategy(), minMALookback, maxMALookback, riskMax, riskIncrement, allowance);
        }
    }

    private static void getMACombos(BuyStrategyType buyStrategyType, SellStrategyType sellStrategyType, int minMALookback, int maxMALookback, BigDecimal riskMax, BigDecimal riskIncrement, BigDecimal allowance) {
        int shortLookback;
        int longLookback = minMALookback;
        while (longLookback <= maxMALookback) {
//            shortLookback = minMALookback;
            shortLookback = Math.max(minMALookback, longLookback - 2); //having a difference of more than x has not proven to be profitable
            while (shortLookback < longLookback) { // there's no need to test equal short and long tail MAs because they will never separate or converge. That's why this is < and not <=.
                getRiskCombos(buyStrategyType, sellStrategyType, riskMax, riskIncrement, shortLookback, longLookback, allowance);
                shortLookback++;
            }
            longLookback++;
        }
    }

    private static void getRiskCombos(BuyStrategyType buyStrategyType, SellStrategyType sellStrategyType, BigDecimal riskMax, BigDecimal riskIncrement, int shortLookback, int longLookback, BigDecimal allowance) {
        BigDecimal highRisk = CONFIG.isBacktestHighRisk() ? BigDecimal.ZERO : CONFIG.getHighRisk();
        do {
            BigDecimal lowRisk = CONFIG.isBacktestLowRisk() ? BigDecimal.ZERO : CONFIG.getLowRisk();
            do {
                processExecutorService.execute(new ProcessHistoricIntradayPrices(buyStrategyType, sellStrategyType, shortLookback, longLookback, highRisk, lowRisk, allowance));
                count++;
                lowRisk = lowRisk.add(riskIncrement);
            } while (CONFIG.isBacktestLowRisk() && lowRisk.compareTo(riskMax) <= 0);
            highRisk = highRisk.add(riskIncrement);
        } while (CONFIG.isBacktestHighRisk() && highRisk.compareTo(riskMax) <= 0);
    }

    private static void logStatus(StopWatch watch, BigDecimal initialBacktestQueueSize, ScheduledExecutorService timer) {
        timer.scheduleAtFixedRate(() -> {
            BacktestParameterResults backtestParameterResults = BACKTEST_RESULTS_QUEUE.stream().max(Comparator.comparing(BacktestParameterResults::getTimestamp)).orElse(null);
            if (backtestParameterResults == null)
                return;
            int currentLong = backtestParameterResults.getLongLookback();
            int currentShort = backtestParameterResults.getShortLookback();
            BigDecimal currentBacktestQueueSize = initialBacktestQueueSize.subtract(BigDecimal.valueOf(processExecutorService.getCompletedTaskCount()));
            BigDecimal percentRemaining = currentBacktestQueueSize.divide(initialBacktestQueueSize, 8, RoundingMode.HALF_UP);
            bestv = BACKTEST_RESULTS_QUEUE.stream().max(Comparator.comparing(BacktestParameterResults::getValue)).orElse(BacktestParameterResults.builder().value(BigDecimal.ZERO).build());
            if (bestv != lastBestv)
                LOG.info(MessageFormat.format("\n\nHomer Simpson: The best combination of parameters SO FAR is\n\t{0}\nwith a value of ${1}\n", bestv, bestv.getValue()));
            lastBestv = bestv;
            System.out.print(MessageFormat.format("{2}/{3} {0,number,percent} {1} remaining. ({4}, {5} - {6})\r", percentRemaining, getRemainingTimeUnits(BigDecimal.valueOf(count), percentRemaining, BigDecimal.valueOf(watch.getTime(TimeUnit.MILLISECONDS))), currentBacktestQueueSize, initialBacktestQueueSize, currentShort, currentLong, bestv.getValue()));
        }, 0, 1, TimeUnit.SECONDS);
    }

    private static void persistComboResults() {
        if (!save)
            return;
        LOG.trace("Writing results to the database. Please wait.");
        List<BacktestParameterResults> rs = new ArrayList<>();
        BacktestParameterResults r;
        while (BACKTEST_RESULTS_QUEUE.peek() != null) {
            r = BACKTEST_RESULTS_QUEUE.poll();
            r.setStart(CONFIG.getBacktestStart());
            r.setEnd(CONFIG.getBacktestEnd());
            rs.add(r);
        }
        backtestParameterResultsRepository.saveAll(rs);
    }

    private static String getRemainingTimeUnits(BigDecimal t, BigDecimal r, BigDecimal c) {
        if (r.compareTo(BigDecimal.ONE) >= 0)
            return "Calculating remaining time...";
        BigDecimal remainingTime = (t.multiply(r)).multiply((c).divide((BigDecimal.ONE.subtract(r)).multiply(t), 8, RoundingMode.HALF_UP));
        Duration timeLeft = Duration.ofMillis(remainingTime.longValue());
        return String.format("%02d:%02d:%02d", timeLeft.toHours(), timeLeft.toMinutesPart(), timeLeft.toSecondsPart());
    }
}
