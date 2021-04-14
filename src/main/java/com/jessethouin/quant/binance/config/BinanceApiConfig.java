package com.jessethouin.quant.binance.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@Setter
public class BinanceApiConfig {
    private static final Logger LOG = LogManager.getLogger(BinanceApiConfig.class);
    InputStream inputStream;
    String userName;
    String apiKey;
    String secretKey;
    String restApi;
    String wsApi;
    String streamApi;

    private BinanceApiConfig() {
        try {
            Properties prop = new Properties();
            String propFileName = "binance-prod.properties";
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath.");
            }
            setUserName(prop.getProperty("userName"));
            setApiKey(prop.getProperty("apiKey"));
            setSecretKey(prop.getProperty("secretKey"));
            setRestApi(prop.getProperty("restApi"));
            setWsApi(prop.getProperty("wsApi"));
            setStreamApi(prop.getProperty("streamApi"));
        } catch (IOException e) {
            LOG.error("Unable to read properties file: " + e.getLocalizedMessage());
        }
    }

    public static final BinanceApiConfig INSTANCE = new BinanceApiConfig();
}
