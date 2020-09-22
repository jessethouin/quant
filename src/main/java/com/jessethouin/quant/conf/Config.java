package com.jessethouin.quant.conf;

import java.math.BigDecimal;

public class Config {
    private BigDecimal initialCash = BigDecimal.valueOf(25000);
    private int shortPeriod = 8;
    private int longPeriod = 20;
    private BigDecimal highRisk = BigDecimal.valueOf(.09);
    private BigDecimal lowRisk = BigDecimal.valueOf(.10);
    private BigDecimal allowance = BigDecimal.valueOf(.10);

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
        this.initialCash = initialCash;
    }

    public int getShortPeriod() {
        return shortPeriod;
    }

    public void setShortPeriod(int shortPeriod) {
        this.shortPeriod = shortPeriod;
    }

    public int getLongPeriod() {
        return longPeriod;
    }

    public void setLongPeriod(int longPeriod) {
        this.longPeriod = longPeriod;
    }

    public BigDecimal getHighRisk() {
        return highRisk;
    }

    public void setHighRisk(BigDecimal highRisk) {
        this.highRisk = highRisk;
    }

    public BigDecimal getLowRisk() {
        return lowRisk;
    }

    public void setLowRisk(BigDecimal lowRisk) {
        this.lowRisk = lowRisk;
    }

    public BigDecimal getAllowance() {
        return allowance;
    }

    public void setAllowance(BigDecimal allowance) {
        this.allowance = allowance;
    }
}
