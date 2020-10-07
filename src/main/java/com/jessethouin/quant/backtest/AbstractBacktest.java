package com.jessethouin.quant.backtest;

import com.jessethouin.quant.conf.Config;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public abstract class AbstractBacktest {
    public static final List<BigDecimal> intradayPrices = new ArrayList<>();

    public static void populateIntradayPrices() {
        Config config = new Config();
        InputStream data = Thread.currentThread().getContextClassLoader().getResourceAsStream(config.getBackTestData());
        if (data == null)
            throw new NullPointerException("data was null for some reason. Perhaps the file didn't exist, or we didn't have permissions to read it.");
        try (Scanner scanner = new Scanner(data)) {
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
}
