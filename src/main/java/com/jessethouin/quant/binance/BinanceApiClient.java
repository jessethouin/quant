package com.jessethouin.quant.binance;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiMarginRestClient;
import com.jessethouin.quant.binance.config.BinanceApiConfig;

public class BinanceApiClient {
    BinanceApiClientFactory factory;
    BinanceApiMarginRestClient client;
    public static final BinanceApiClient BINANCE_API_CLIENT = new BinanceApiClient();

    private BinanceApiClient() {
        factory = BinanceApiClientFactory.newInstance(BinanceApiConfig.INSTANCE.getApiKey(), BinanceApiConfig.INSTANCE.getSecretKey());
        client = factory.newMarginRestClient();
    }
}
