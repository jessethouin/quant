package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.config.BinanceApiConfig;
import com.jessethouin.quant.broker.Util;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.jessethouin.quant.conf.Config.CONFIG;

public class BinanceExchangeServices {
    public static final BinanceApiConfig BINANCE_API_CONFIG = BinanceApiConfig.INSTANCE;
    public static final Map<CurrencyPair, BigDecimal> BINANCE_MIN_TRADES = new HashMap<>();
    public static final BinanceExchange BINANCE_EXCHANGE;
    public static final BinanceStreamingExchange BINANCE_STREAMING_EXCHANGE;
    public static final BinanceExchangeInfo BINANCE_EXCHANGE_INFO;
    public static final BinanceMarketDataService BINANCE_MARKET_DATA_SERVICE;
    public static final BinanceTradeService BINANCE_TRADE_SERVICE;
    public static final BinanceAccountService BINANCE_ACCOUNT_SERVICE;

    static {
        ExchangeSpecification exSpec = new BinanceExchange().getDefaultExchangeSpecification();
        exSpec.setUserName(BINANCE_API_CONFIG.getUserName());
        exSpec.setApiKey(BINANCE_API_CONFIG.getApiKey());
        exSpec.setSecretKey(BINANCE_API_CONFIG.getSecretKey());
        BINANCE_EXCHANGE = (BinanceExchange) ExchangeFactory.INSTANCE.createExchange(exSpec);

        ExchangeSpecification strExSpec = new BinanceStreamingExchange().getDefaultExchangeSpecification();
        strExSpec.setUserName(BINANCE_API_CONFIG.getUserName());
        strExSpec.setApiKey(BINANCE_API_CONFIG.getApiKey());
        strExSpec.setSecretKey(BINANCE_API_CONFIG.getSecretKey());
        BINANCE_STREAMING_EXCHANGE = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(strExSpec);

        BINANCE_EXCHANGE_INFO = BINANCE_EXCHANGE.getExchangeInfo();
        BINANCE_MARKET_DATA_SERVICE = (BinanceMarketDataService) BINANCE_EXCHANGE.getMarketDataService();
        BINANCE_TRADE_SERVICE = (BinanceTradeService) BINANCE_EXCHANGE.getTradeService();
        BINANCE_ACCOUNT_SERVICE = (BinanceAccountService) BINANCE_EXCHANGE.getAccountService();

        Util.getAllCurrencyPairs(CONFIG).forEach(currencyPair -> BINANCE_MIN_TRADES.put(currencyPair, BinanceUtil.getMinTrade(currencyPair)));
    }
}