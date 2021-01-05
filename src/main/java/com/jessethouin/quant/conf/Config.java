package com.jessethouin.quant.conf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class);
    InputStream inputStream;

    private BigDecimal initialCash;
    private BigDecimal highRisk;
    private BigDecimal lowRisk;
    private BigDecimal allowance;
    private BigDecimal gain;
    private BigDecimal loss;
    private int shortLookback;
    private int longLookback;
    private BuyStrategyTypes buyStrategy;
    private SellStrategyTypes sellStrategy;
    private boolean backTest;
    private String backTestData;
    private boolean backTestDB;
    private int backtestQty;
    private List<String> securities;
    private List<String> fiatCurrencies;
    private List<String> cryptoCurrencies;
    private Broker broker;

    public static final Config INSTANCE = new Config();

    private Config() {
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
            setGain(new BigDecimal(prop.getProperty("gain")));
            setLoss(new BigDecimal(prop.getProperty("loss")));
            setHighRisk(new BigDecimal(prop.getProperty("highRisk")));
            setLowRisk(new BigDecimal(prop.getProperty("lowRisk")));
            setShortLookback(Integer.parseInt(prop.getProperty("shortLookback")));
            setLongLookback(Integer.parseInt(prop.getProperty("longLookback")));
            setBuyStrategy(BuyStrategyTypes.valueOf(prop.getProperty("buyStrategy")));
            setSellStrategy(SellStrategyTypes.valueOf(prop.getProperty("sellStrategy")));
            setBackTest(Boolean.parseBoolean(prop.getProperty("backTest")));
            setBackTestData(prop.getProperty("backTestData"));
            setBackTestDB(Boolean.parseBoolean(prop.getProperty("backTestDB")));
            setBacktestQty(Integer.parseInt(prop.getProperty("backtestQty")));
            setSecurities(Stream.of(prop.getProperty("securities").split(",", -1)).collect(Collectors.toList()));
            setFiatCurrencies(Stream.of(prop.getProperty("fiatCurrencies").split(",", -1)).collect(Collectors.toList()));
            setCryptoCurrencies(Stream.of(prop.getProperty("cryptoCurrencies").split(",", -1)).collect(Collectors.toList()));
            setBroker(Broker.valueOf(prop.getProperty("broker")));
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

    public BigDecimal getLoss() {
        return loss;
    }

    public void setLoss(BigDecimal loss) {
        this.loss = loss;
    }

    public BigDecimal getGain() {
        return gain;
    }

    public void setGain(BigDecimal gain) {
        this.gain = gain;
    }

    public void setInitialCash(BigDecimal initialCash) {
        this.initialCash = initialCash;
    }

    public int getShortLookback() {
        return shortLookback;
    }

    public void setShortLookback(int shortLookback) {
        this.shortLookback = shortLookback;
    }

    public int getLongLookback() {
        return longLookback;
    }

    public void setLongLookback(int longLookback) {
        this.longLookback = longLookback;
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

    public boolean getBackTest() {
        return backTest;
    }

    public void setBackTest(boolean backTest) {
        this.backTest = backTest;
    }

    public String getBackTestData() {
        return backTestData;
    }

    public void setBackTestData(String backTestData) {
        this.backTestData = backTestData;
    }

    public boolean isBackTestDB() {
        return backTestDB;
    }

    public void setBackTestDB(boolean backTestDB) {
        this.backTestDB = backTestDB;
    }

    public int getBacktestQty() {
        return backtestQty;
    }

    public void setBacktestQty(int backtestQty) {
        this.backtestQty = backtestQty;
    }

    public List<String> getSecurities() {
        return securities;
    }

    public void setSecurities(List<String> securities) {
        this.securities = securities;
    }

    public List<String> getFiatCurrencies() {
        return fiatCurrencies;
    }

    public void setFiatCurrencies(List<String> fiatCurrencies) {
        this.fiatCurrencies = fiatCurrencies;
    }

    public List<String> getCryptoCurrencies() {
        return cryptoCurrencies;
    }

    public void setCryptoCurrencies(List<String> cryptoCurrencies) {
        this.cryptoCurrencies = cryptoCurrencies;
    }

    public Broker getBroker() {
        return broker;
    }

    public void setBroker(Broker broker) {
        this.broker = broker;
    }

    public static Config getTheadSafeConfig() {
        return new Config();
    }
}
