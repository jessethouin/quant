package com.jessethouin.quant.conf;

import lombok.Getter;
import lombok.Setter;
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

@Getter
@Setter
public class Config {
    private static final Logger LOG = LogManager.getLogger(Config.class);
    InputStream inputStream;

    private BigDecimal initialCash;
    private BigDecimal highRisk;
    private BigDecimal lowRisk;
    private BigDecimal allowance;
    private BigDecimal gain;
    private BigDecimal loss;
    private BigDecimal stopLoss;
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
    private DataFeed dataFeed;

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
            setStopLoss(new BigDecimal(prop.getProperty("stopLoss")));
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
            setDataFeed(DataFeed.valueOf(prop.getProperty("dataFeed")));
        } catch (IOException e) {
            LOG.error("Unable to read properties file: " + e.getLocalizedMessage());
        }
    }

    public static Config getTheadSafeConfig() {
        return new Config();
    }
}
