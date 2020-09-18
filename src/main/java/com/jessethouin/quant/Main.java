package com.jessethouin.quant;

import com.jessethouin.quant.calculators.DEMA;
import com.jessethouin.quant.calculators.SMA;
import com.jessethouin.quant.calculators.TEMA;
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
            e.printStackTrace();
        }

        List<BigDecimal> shortOutput = movingAverage(intradayPrices, Config.getShortPeriod());
        List<BigDecimal> longOutput = movingAverage(intradayPrices, Config.getLongPeriod());

        Portfolio portfolio = new Portfolio();
        portfolio.addCash(Config.getInitialCash());

        Security aapl = new Security("AAPL");
        portfolio.getSecurities().add(aapl);

        BigDecimal beginningPrice = intradayPrices.get(0);
        Calc c = new Calc(aapl, beginningPrice, beginningPrice, beginningPrice, BigDecimal.ZERO, true);

        for (int i = 0; i < intradayPrices.size(); i++) {
            LOG.debug(i + ": " + shortOutput.get(i) + " : " + longOutput.get(i) + " : " + intradayPrices.get(i) + " : " + portfolio.getCash());
            c.price = intradayPrices.get(i);
            c.qty = portfolio.getCash().multiply(BigDecimal.valueOf(.10)).divide(c.price, 0, RoundingMode.HALF_UP);
            c.dema1 = shortOutput.get(i);
            c.dema2 = longOutput.get(i);
            portfolio.addCash(c.decide());
        }

        LOG.debug(portfolio.toString());
        LOG.debug(portfolio.getPortfolioValue());
    }

    static class Calc {
        private final Security security;
        private BigDecimal price;
        private BigDecimal dema1;
        private BigDecimal dema2;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal spread;
        private boolean buy;
        private BigDecimal qty;

        public Calc(Security security, BigDecimal price, BigDecimal high, BigDecimal low, BigDecimal spread, boolean buy) {
            this.security = security;
            this.price = price;
            this.high = high;
            this.low = low;
            this.spread = spread;
            this.buy = buy;
        }

        public BigDecimal decide() {
            BigDecimal proceeds = BigDecimal.ZERO;

            if (price.compareTo(high) > 0) {
                high = price;
            } else if (price.compareTo(low) < 0) {
                low = price;
                high = price;
                spread = high.subtract(low);
                //resetLasts();
                buy = true;
                return proceeds;
            }

            spread = high.subtract(low);

            if (dema1.signum() == 0 || dema2.signum() == 0) return proceeds;

            if (dema1.compareTo(dema2) > 0 && dema1.compareTo(low.add(spread.multiply(Config.getLowRisk()))) > 0 && dema1.compareTo(high.subtract(spread.multiply(Config.getHighRisk()))) < 0 && buy) {
                proceeds = security.buySecurity(qty, price).negate();
                buy = false;
            } else if (dema1.compareTo(dema2) < 0 && price.compareTo(high.subtract(spread.multiply(Config.getHighRisk()))) < 0) {
                proceeds = security.sellSecurity(price);
                buy = true;
            }

            return proceeds;
        }
    }

    public static List<BigDecimal> movingAverage(List<BigDecimal> in, int period) {
        List<BigDecimal> output = new ArrayList<>();
        BigDecimal previous = BigDecimal.ZERO;
        for (int i = 0; i < in.size(); i++) {
            if (i < period) {
                output.add(BigDecimal.ZERO);
                continue;
            }
            if (i == period) {
                previous = SMA.sma(in.subList(0, period), period);
                output.add(BigDecimal.ZERO);
                continue;
            }
            previous = DEMA.dema(in.get(i), previous, period);
//            previous = TEMA.tema(in.get(i), previous, period);
            output.add(previous);
        }

        return output;
    }
}
