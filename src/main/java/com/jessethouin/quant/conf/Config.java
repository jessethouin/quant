package com.jessethouin.quant.conf;

import java.math.BigDecimal;

public class Config {
    private static int shortPeriod = 8;
    private static int longPeriod = 20;
    private static BigDecimal initialCash = BigDecimal.valueOf(25000);
    private static BigDecimal highRisk = BigDecimal.valueOf(.09);
    private static BigDecimal lowRisk = BigDecimal.valueOf(.10);

    public static BigDecimal getInitialCash() {
        return initialCash;
    }

    public static void setInitialCash(BigDecimal initialCash) {
        Config.initialCash = initialCash;
    }

    public static int getShortPeriod() {
        return shortPeriod;
    }

    public static void setShortPeriod(int shortPeriod) {
        Config.shortPeriod = shortPeriod;
    }

    public static int getLongPeriod() {
        return longPeriod;
    }

    public static void setLongPeriod(int longPeriod) {
        Config.longPeriod = longPeriod;
    }

    public static BigDecimal getHighRisk() {
        return highRisk;
    }

    public static void setHighRisk(BigDecimal highRisk) {
        Config.highRisk = highRisk;
    }

    public static BigDecimal getLowRisk() {
        return lowRisk;
    }

    public static void setLowRisk(BigDecimal lowRisk) {
        Config.lowRisk = lowRisk;
    }
}
