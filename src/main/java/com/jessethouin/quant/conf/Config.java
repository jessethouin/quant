package com.jessethouin.quant.conf;

import java.math.BigDecimal;

public class Config {
    private static BigDecimal initialCash = BigDecimal.valueOf(25000);
    private static int shortPeriod = 8;
    private static int longPeriod = 20;
    private static BigDecimal highRisk = BigDecimal.valueOf(.09);
    private static BigDecimal lowRisk = BigDecimal.valueOf(.10);
    private static BigDecimal allowance = BigDecimal.valueOf(.10);

    public Config(int shortPeriod, int longPeriod, BigDecimal highRisk, BigDecimal lowRisk) {
        setShortPeriod(shortPeriod);
        setLongPeriod(longPeriod);
        setHighRisk(highRisk);
        setLowRisk(lowRisk);
    }

    public BigDecimal getInitialCash() {
        return initialCash;
    }

    public void setInitialCash(BigDecimal initialCash) {
        Config.initialCash = initialCash;
    }

    public int getShortPeriod() {
        return shortPeriod;
    }

    public void setShortPeriod(int shortPeriod) {
        Config.shortPeriod = shortPeriod;
    }

    public int getLongPeriod() {
        return longPeriod;
    }

    public void setLongPeriod(int longPeriod) {
        Config.longPeriod = longPeriod;
    }

    public BigDecimal getHighRisk() {
        return highRisk;
    }

    public void setHighRisk(BigDecimal highRisk) {
        Config.highRisk = highRisk;
    }

    public BigDecimal getLowRisk() {
        return lowRisk;
    }

    public void setLowRisk(BigDecimal lowRisk) {
        Config.lowRisk = lowRisk;
    }

    public static BigDecimal getAllowance() {
        return allowance;
    }

    public static void setAllowance(BigDecimal allowance) {
        Config.allowance = allowance;
    }
}
