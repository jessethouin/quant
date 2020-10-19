package com.jessethouin.quant.backtest;

import com.jessethouin.quant.conf.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(AbstractBacktest.class);
    public static final List<BigDecimal> intradayPrices = new ArrayList<>();

    public static void populateIntradayPrices() {
        Config config = new Config();
        LOG.info("Loading historic trades from " + config.getBackTestData());
        InputStream data = Thread.currentThread().getContextClassLoader().getResourceAsStream(config.getBackTestData());
        if (data == null)
            throw new NullPointerException("data was null for some reason. Perhaps the file didn't exist, or we didn't have permissions to read it.");
        try (Scanner scanner = new Scanner(data)) {
            while (scanner.hasNextLine()) {
                try (Scanner rowScanner = new Scanner(scanner.nextLine())) {
                    rowScanner.useDelimiter(",");
                    if (rowScanner.hasNext()) {
                        rowScanner.next();
                        intradayPrices.add(rowScanner.nextBigDecimal());
                    }
                }
            }
        }
        LOG.info("Finished loading trades from " + config.getBackTestData());
//        Collections.reverse(intradayPrices);
    }
}
