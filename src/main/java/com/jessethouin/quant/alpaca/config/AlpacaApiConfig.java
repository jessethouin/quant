package com.jessethouin.quant.alpaca.config;

import com.jessethouin.quant.binance.config.BinanceApiConfig;
import lombok.Getter;
import lombok.Setter;
import net.jacobpeterson.alpaca.model.util.apitype.MarketDataWebsocketSourceType;
import net.jacobpeterson.alpaca.model.util.apitype.TraderAPIEndpointType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Getter
@Setter
public class AlpacaApiConfig {
    private static final Logger LOG = LogManager.getLogger(BinanceApiConfig.class);
    InputStream inputStream;
    TraderAPIEndpointType endpointApiType;
    MarketDataWebsocketSourceType dataApiType;
    String userAgent;
    String keyId;
    String secretKey;

    public AlpacaApiConfig() {
        try {
            Properties prop = new Properties();
            String propFileName = "alpaca.properties";
            inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
            if (inputStream != null) {
                prop.load(inputStream);
            } else {
                throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath.");
            }
            setEndpointApiType(TraderAPIEndpointType.fromValue(prop.getProperty("endpoint_api_type")));
            setDataApiType(MarketDataWebsocketSourceType.fromValue(prop.getProperty("data_api_type")));
            setUserAgent(prop.getProperty("user_agent"));
            setKeyId(prop.getProperty("key_id"));
            setSecretKey(prop.getProperty("secret_key"));
        } catch (IOException e) {
            LOG.error("Unable to read properties file: " + e.getLocalizedMessage());
        } catch (IllegalArgumentException e) {
            LOG.error("Error assigning APIType: " + e.getLocalizedMessage());
        } catch (Exception e) {
            LOG.error("Error configuring AlpacaApiConfig: " + e.getLocalizedMessage());
        }
    }

    public static final AlpacaApiConfig INSTANCE = new AlpacaApiConfig();
}
