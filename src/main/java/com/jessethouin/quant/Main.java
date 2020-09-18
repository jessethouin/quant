package com.jessethouin.quant;

import com.jessethouin.quant.calculators.*;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.exceptions.CashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws CashException {
        List<BigDecimal> intradayPrices = new ArrayList<>();

        try (Scanner scanner = new Scanner(new File("./AAPL.csv"))) {
            while (scanner.hasNextLine()) {
                try (Scanner rowScanner = new Scanner(scanner.nextLine())) {
                    rowScanner.useDelimiter(",");
                    while (rowScanner.hasNext()) {
                        try {
                            rowScanner.next();
                            intradayPrices.add(rowScanner.nextBigDecimal());
                        } catch (InputMismatchException ime) {
                            LOG.error("Hit InputMismatchException: " + ime.getLocalizedMessage());
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage());
        }

        Portfolio portfolio = new Portfolio();
        portfolio.addCash(Config.getInitialCash());

        Security aapl = new Security("AAPL");
        portfolio.getSecurities().add(aapl);

        Calc c = new Calc(aapl, intradayPrices.get(0));

        List<BigDecimal> shortOutput = MA.ma(intradayPrices, Config.getShortPeriod(), MATypes.DEMA);
        List<BigDecimal> longOutput = MA.ma(intradayPrices, Config.getLongPeriod(), MATypes.DEMA);

        for (int i = 0; i < intradayPrices.size(); i++) {
            LOG.debug(i + ": " + shortOutput.get(i) + " : " + longOutput.get(i) + " : " + intradayPrices.get(i) + " : " + portfolio.getCash());
            c.updateCalc(intradayPrices.get(i), shortOutput.get(i), longOutput.get(i), portfolio);
            portfolio.addCash(c.decide());
        }

        LOG.debug(portfolio.toString());
        LOG.debug(portfolio.getPortfolioValue());
    }

}
