package com.jessethouin.quant.binance;

import static com.jessethouin.quant.conf.Config.CONFIG;

import com.jessethouin.quant.binance.config.BinanceApiConfig;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.binance.BinanceStreamingMarketDataService;
import info.bitrich.xchangestream.binance.BinanceStreamingTradeService;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.currency.CurrencyPair;

public class BinanceExchangeServices {
    public static final BinanceApiConfig BINANCE_API_CONFIG = BinanceApiConfig.INSTANCE;
    public static final Map<CurrencyPair, BigDecimal> BINANCE_MIN_TRADES = new HashMap<>();
    public static final BinanceExchange BINANCE_EXCHANGE;
    public static final BinanceStreamingExchange BINANCE_STREAMING_EXCHANGE;
    public static final BinanceExchangeInfo BINANCE_EXCHANGE_INFO;
    public static final BinanceMarketDataService BINANCE_MARKET_DATA_SERVICE;
    public static final BinanceStreamingMarketDataService BINANCE_STREAMING_MARKET_DATA_SERVICE;
    public static final BinanceStreamingTradeService BINANCE_STREAMING_TRADE_SERVICE;
    public static final BinanceTradeService BINANCE_TRADE_SERVICE;
    public static final BinanceAccountService BINANCE_ACCOUNT_SERVICE;

    static {
        /* REST API Exchange */
        BINANCE_EXCHANGE = (BinanceExchange) ExchangeFactory.INSTANCE.createExchange(configureExchangeSpec(new BinanceExchange().getDefaultExchangeSpecification()));
        BINANCE_EXCHANGE_INFO = BINANCE_EXCHANGE.getExchangeInfo();
        BINANCE_MARKET_DATA_SERVICE = (BinanceMarketDataService) BINANCE_EXCHANGE.getMarketDataService();
        BINANCE_TRADE_SERVICE = (BinanceTradeService) BINANCE_EXCHANGE.getTradeService();
        BINANCE_ACCOUNT_SERVICE = (BinanceAccountService) BINANCE_EXCHANGE.getAccountService();

        /* Streaming API Exchange */
        BINANCE_STREAMING_EXCHANGE = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(configureStreamingExchangeSpec(new BinanceStreamingExchange().getDefaultExchangeSpecification()));

        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();
        List<CurrencyPair> currencyPairs = BinanceUtil.getAllCryptoCurrencyPairs(CONFIG);
        currencyPairs.forEach(productSubscriptionBuilder::addAll);

        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();
        BINANCE_STREAMING_MARKET_DATA_SERVICE = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService();
        BINANCE_STREAMING_TRADE_SERVICE = BINANCE_STREAMING_EXCHANGE.getStreamingTradeService();
        BinanceUtil.getAllCryptoCurrencyPairs(CONFIG).forEach(currencyPair -> BINANCE_MIN_TRADES.put(currencyPair, BinanceUtil.getMinTrade(currencyPair)));
    }

    private static ExchangeSpecification configureExchangeSpec(ExchangeSpecification exSpec) {
        exSpec.setSslUri(BINANCE_API_CONFIG.getRestApi());
        exSpec.setHost("www.binance.com");
        exSpec.setPort(80);
        exSpec.setExchangeName("Binance");
        exSpec.setExchangeDescription("Binance Exchange.");
        exSpec.setUserName(BINANCE_API_CONFIG.getUserName());
        exSpec.setApiKey(BINANCE_API_CONFIG.getApiKey());
        exSpec.setSecretKey(BINANCE_API_CONFIG.getSecretKey());
        return exSpec;
    }

    private static ExchangeSpecification configureStreamingExchangeSpec(ExchangeSpecification exSpec) {
        exSpec.setSslUri(BINANCE_API_CONFIG.getRestApi());
        exSpec.setUserName(BINANCE_API_CONFIG.getUserName());
        exSpec.setApiKey(BINANCE_API_CONFIG.getApiKey());
        exSpec.setSecretKey(BINANCE_API_CONFIG.getSecretKey());
        return exSpec;
    }
}