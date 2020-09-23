package com.jessethouin.quant;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.math.BigDecimal;
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

        int max = 30;
        BigDecimal rmax = BigDecimal.valueOf(.25);
        BigDecimal hlIncrement = BigDecimal.valueOf(.05);

        if (args.length == 0) LOG.info("Using default values for max (30), rmax (.25), and hlIncrement (.05).");

        for (int i = 0; i < args.length; i++) {
            switch (i) {
                case 0 -> max = Integer.parseInt(args[i]);
                case 1 -> rmax = BigDecimal.valueOf(Double.parseDouble(args[i]));
                case 2 -> hlIncrement = BigDecimal.valueOf(Double.parseDouble(args[i]));
            }
        }

/*
        Portfolio portfolio = new Portfolio();
        portfolio.setCash(Config.getInitialCash());

        Security aapl = new Security("AAPL");
        Security goog = new Security("GOOG"); // TODO: ability to work with multiple securities

        portfolio.setSecurities(new ArrayList<>(Collections.singletonList(aapl)));
*/

        populateIntradayPrices();

        int s;
        int l = 0;
        BigDecimal rh;
        BigDecimal rl;
        int combos = 0;

        while (l < max) {
            l++;
            s = 0;
            while (s < l) {
                s++;
                rh = BigDecimal.ZERO;
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
                    List<Future<Object>> answers = es.invokeAll(todo);
                    es.shutdown();
                    es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                }
            }
        }

        LOG.info(MessageFormat.format("{0} combinations were tested.", combos));
        LOG.info(MessageFormat.format("\n\nThe best combination of parameters is\n\t{0}\nwith a value of ${1}\n", getBest(), getBestv()));

        watch.stop();
        LOG.info(MessageFormat.format("Time Elapsed: {0}", watch.getTime(TimeUnit.MILLISECONDS) / 1000));

//        ImageCharts line = new ImageCharts().cht("ls").chd()
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
