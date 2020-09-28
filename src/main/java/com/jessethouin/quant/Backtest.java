package com.jessethouin.quant;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Backtest {
    private static final Logger LOG = LogManager.getLogger(Backtest.class);
    public static final List<BigDecimal> intradayPrices = new ArrayList<>();
    static String best = "";
    static BigDecimal bestv = BigDecimal.ZERO;

    static void findBestCombos(String[] args) throws InterruptedException {
        StopWatch watch = new StopWatch();
        watch.start();

        int start = 0;
        int max = 10;
        BigDecimal rmax = BigDecimal.valueOf(.10);
        BigDecimal hlIncrement = BigDecimal.valueOf(.01);

        if (args.length != 0 && args.length != 4) {
            LOG.error("Listen. You either need to supply 4 arguments or none. Stop trying to half-ass this thing. start, max, rmax, and hlIncrement if you need specifics. Otherwise, shut up and let me do the work.");
            return;
        }

        if (args.length == 0) LOG.info(MessageFormat.format("Using default values for start {0}, max {1}, rmax {2}, and hlIncrement {3}.", start, max, rmax, hlIncrement));

        for (int i = 0; i < args.length; i++) {
            switch (i) {
                case 0 -> start = Integer.parseInt(args[i]);
                case 1 -> max = Integer.parseInt(args[i]);
                case 2 -> rmax = BigDecimal.valueOf(Double.parseDouble(args[i]));
                case 3 -> hlIncrement = BigDecimal.valueOf(Double.parseDouble(args[i]));
            }
        }

        if (start > max) {
            LOG.error(MessageFormat.format("Starting moving average count {0} must be less than max {1}", start, max));
            return;
        }

        populateIntradayPrices();

        LOG.info(MessageFormat.format("{0} combinations were tested.", getMACombos(start, max, rmax, hlIncrement)));
        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        watch.stop();
        LOG.info(MessageFormat.format("Time Elapsed: {0}", watch.getTime(TimeUnit.MILLISECONDS) / 1000));

        return;
    }

    private static int getMACombos(int start, int max, BigDecimal rmax, BigDecimal hlIncrement) throws InterruptedException {
        int s;
        int l = 0, c = 0, combos = 0;
        int i = ((max * (max + 1)) / 2) - max;
        int i_ = i;
        BigDecimal rh;
        List<Long> nt_ = new ArrayList<>();
        StopWatch loopWatch = new StopWatch();

        while (++l <= max) {
            s = start;
            while (++s < l) { // there's no need to test equal short and long tail MAs because they will never separate or converge. That's why this is < and not <=.
                rh = BigDecimal.ZERO;
                combos = getRiskCombos(rmax, hlIncrement, s, l, combos, rh, nt_, loopWatch);

                double[] timeUntis = getRemainingTimeUnits(nt_, --i_);
                LOG.info(MessageFormat.format("{0,number,percent} complete. {1,number,00:}{2,number,00:}{3,number,00.00} remaining", ++c / (float) i, timeUntis[0], timeUntis[1], timeUntis[2]));
            }
        }
        return combos;
    }

    private static int getRiskCombos(BigDecimal rmax, BigDecimal hlIncrement, int s, int l, int combos, BigDecimal rh, List<Long> nt_, StopWatch loopWatch) throws InterruptedException {
        long nt;
        BigDecimal rl;
        loopWatch.start();
        while (rh.compareTo(rmax) < 0) {
            rh = rh.add(hlIncrement);
            rl = BigDecimal.ZERO;
            ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            List<Callable<Object>> todo = new ArrayList<>();
            while (rl.compareTo(rmax) < 0) {
                combos++;
                rl = rl.add(hlIncrement);
                todo.add(new ProcessHistoricIntradayPrices(s, l, rh, rl, intradayPrices));
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

    private static void populateIntradayPrices() {
        InputStream aapl_csv = Thread.currentThread().getContextClassLoader().getResourceAsStream("AAPL.csv");
        if (aapl_csv == null)
            throw new NullPointerException("aapl_csv was null for some reason. Perhaps the file didn't exist, or we didn't have permissions to read it.");
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
    }

    public static String getBest() {
        return best;
    }

    public static synchronized void setBest(String best) {
        Backtest.best = best;
    }

    public static BigDecimal getBestv() {
        return bestv;
    }

    public static synchronized void setBestv(BigDecimal bestv) {
        Backtest.bestv = bestv;
        LOG.info(MessageFormat.format("\n\nHomer Simpson: The best combination of parameters SO FAR is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));
    }

    public static synchronized void updateBest(String msg, BigDecimal v) {
        if (v.compareTo(getBestv()) > 0) {
            setBest(msg);
            setBestv(v);
        }
    }
}
