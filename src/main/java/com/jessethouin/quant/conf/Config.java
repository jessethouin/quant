package com.jessethouin.quant.conf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Properties;

public class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class);
    InputStream inputStream;

    private BigDecimal initialCash;
    private BigDecimal highRisk;
    private BigDecimal lowRisk;
    private BigDecimal allowance;
    private int shortMA;
    private int longMA;
    private BuyStrategyTypes buyStrategy;
    private SellStrategyTypes sellStrategy;
    private String backTestData;

    public Config() {
        try {
            Properties prop = new Properties();
            String propFileName = "quant.properties";
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath.");
            }
            setInitialCash(new BigDecimal(prop.getProperty("initialCash")));
            setAllowance(new BigDecimal(prop.getProperty("allowance")));
            setHighRisk(new BigDecimal(prop.getProperty("highRisk")));
            setLowRisk(new BigDecimal(prop.getProperty("lowRisk")));
            setShortMA(Integer.parseInt(prop.getProperty("shortMA")));
            setLongMA(Integer.parseInt(prop.getProperty("longMA")));
            setBuyStrategy(BuyStrategyTypes.valueOf(prop.getProperty("buyStrategy")));
            setSellStrategy(SellStrategyTypes.valueOf(prop.getProperty("sellStrategy")));
            setBackTestData(prop.getProperty("backTestData"));
        } catch (IOException e) {
            LOG.error("Unable to read properties file: " + e.getLocalizedMessage());
        }
    }

    public BigDecimal getInitialCash() {
        return initialCash;
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

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash;
    }

    public int getShortMA() {
        return shortMA;
    }

    public void setShortMA(int shortMA) {
        this.shortMA = shortMA;
    }

    public int getLongMA() {
        return longMA;
    }

    public void setLongMA(int longMA) {
        this.longMA = longMA;
    }

    public BuyStrategyTypes getBuyStrategy() {
        return buyStrategy;
    }

    public void setBuyStrategy(BuyStrategyTypes buyStrategy) {
        this.buyStrategy = buyStrategy;
    }

    public SellStrategyTypes getSellStrategy() {
        return sellStrategy;
    }

    public void setSellStrategy(SellStrategyTypes sellStrategy) {
        this.sellStrategy = sellStrategy;
    }

    public String getBackTestData() {
        return backTestData;
    }

    public void setBackTestData(String backTestData) {
        this.backTestData = backTestData;
    }
}
