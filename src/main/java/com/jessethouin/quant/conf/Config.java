package com.jessethouin.quant.conf;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
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
    private BigDecimal stopLoss;
    private BigDecimal fee;
    private int shortLookback;
    private int longLookback;
    private BuyStrategyTypes buyStrategy;
    private SellStrategyTypes sellStrategy;
    private boolean backTest;
    private int backtestQty;
    private Date backtestStart;
    private Date backtestEnd;
    private boolean backtestLowRisk;
    private boolean backtestHighRisk;
    private boolean backtestAllowance;
    private boolean backtestStrategy;
    private boolean recalibrate;
    private int recalibrateFreq;
    private int recalibrateHours;
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
            setStopLoss(new BigDecimal(prop.getProperty("stopLoss")));
            setFee(new BigDecimal(prop.getProperty("fee")));
            setHighRisk(new BigDecimal(prop.getProperty("highRisk")));
            setLowRisk(new BigDecimal(prop.getProperty("lowRisk")));
            setShortLookback(Integer.parseInt(prop.getProperty("shortLookback")));
            setLongLookback(Integer.parseInt(prop.getProperty("longLookback")));
            setBuyStrategy(BuyStrategyTypes.valueOf(prop.getProperty("buyStrategy")));
            setSellStrategy(SellStrategyTypes.valueOf(prop.getProperty("sellStrategy")));
            setBackTest(Boolean.parseBoolean(prop.getProperty("backTest")));
            setBacktestQty(Integer.parseInt(prop.getProperty("backtestQty")));
            setBacktestStart(Date.from(LocalDateTime.parse(prop.getProperty("backtestStart")).toInstant(ZoneOffset.UTC)));
            setBacktestEnd(Date.from(LocalDateTime.parse(prop.getProperty("backtestEnd")).toInstant(ZoneOffset.UTC)));
            setBacktestLowRisk(Boolean.parseBoolean(prop.getProperty("backtestLowRisk")));
            setBacktestHighRisk(Boolean.parseBoolean(prop.getProperty("backtestHighRisk")));
            setBacktestAllowance(Boolean.parseBoolean(prop.getProperty("backtestAllowance")));
            setBacktestStrategy(Boolean.parseBoolean(prop.getProperty("backtestStrategy")));
            setRecalibrate(Boolean.parseBoolean(prop.getProperty("recalibrate")));
            setRecalibrateFreq(Integer.parseInt(prop.getProperty("recalibrateFreq")));
            setRecalibrateHours(Integer.parseInt(prop.getProperty("recalibrateHours")));
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
