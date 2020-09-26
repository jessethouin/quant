package com.jessethouin.quant;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static String best = "";
    private static BigDecimal bestv = BigDecimal.ZERO;

    public static final List<BigDecimal> intradayPrices = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        StopWatch watch = new StopWatch();
        watch.start();

        int start = 0;
        int max = 10;
        BigDecimal rmax = BigDecimal.valueOf(.10);
        BigDecimal hlIncrement = BigDecimal.valueOf(.01);

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

/*
        Portfolio portfolio = new Portfolio();
        portfolio.setCash(Config.getInitialCash());

        Security aapl = new Security("AAPL");
        Security goog = new Security("GOOG"); // TODO: ability to work with multiple securities

        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(aapl)));
*/

        populateIntradayPrices();

        int combos = getCombos(start, max, rmax, hlIncrement);

        LOG.info(MessageFormat.format("{0} combinations were tested.", combos));
        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        watch.stop();
        LOG.info(MessageFormat.format("Time Elapsed: {0}", watch.getTime(TimeUnit.MILLISECONDS) / 1000));

//        ImageCharts line = new ImageCharts().cht("ls").chd()
    }

    private static int getCombos(int start, int max, BigDecimal rmax, BigDecimal hlIncrement) throws InterruptedException {
        int s;
        int l = 0;
        int c = 0;
        int i = ((max * (max + 1)) / 2) - max;
        int i_ = i;
        double secondsLeft = 0;
        BigDecimal rh;
        BigDecimal rl;
        int combos = 0;
        long nt;
        List<Long> nt_ = new ArrayList<>();
        StopWatch loopWatch = new StopWatch();

        while (++l <= max) {
            s = start;
            while (++s < l) { // there's no need to test short and long tail MAs because they will never separate or converge.
                rh = BigDecimal.ZERO;
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

                secondsLeft = secondsLeft == 0 ? nt / (double)1000 : secondsLeft;
                double ntAvg = nt_.stream().mapToLong(a -> a).average().orElse(0);
                double r = (ntAvg / (double)1000) * --i_;
                r = r % (24 * 3600);
                double hour = r / 3600;
                r %= 3600;
                double minutes = r / 60 ;
                r %= 60;
                double seconds = r;

                LOG.debug(MessageFormat.format("Average execution time for {3} threads (s:{1} l:{2}) is {0} seconds per thread.", nt, s, l, rmax.divide(hlIncrement, RoundingMode.HALF_UP)));
                LOG.info(MessageFormat.format("{0,number,percent} complete. {1,number,00:}{2,number,00:}{3,number,00.00} seconds remaining", ++c / (float) i, hour, minutes, seconds));
            }
        }
        return combos;
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
        Main.best = best;
    }

    public static BigDecimal getBestv() {
        return bestv;
    }

    public static synchronized void setBestv(BigDecimal bestv) {
        Main.bestv = bestv;
        LOG.info(MessageFormat.format("\n\nHomer Simpson: The best combination of parameters SO FAR is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));
    }

    public static synchronized void updateBest(String msg, BigDecimal v) {
        if (v.compareTo(Main.getBestv()) > 0) {
            Main.setBest(msg);
            Main.setBestv(v);
        }
    }
}
