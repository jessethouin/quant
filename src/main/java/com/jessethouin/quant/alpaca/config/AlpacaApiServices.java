package com.jessethouin.quant.alpaca.config;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.openapi.marketdata.api.CryptoApi;
import net.jacobpeterson.alpaca.openapi.marketdata.api.StockApi;
import net.jacobpeterson.alpaca.openapi.trader.api.AccountActivitiesApi;
import net.jacobpeterson.alpaca.openapi.trader.api.AccountsApi;
import net.jacobpeterson.alpaca.openapi.trader.api.OrdersApi;
import net.jacobpeterson.alpaca.openapi.trader.api.PositionsApi;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.crypto.CryptoMarketDataWebsocketInterface;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataWebsocketInterface;
import net.jacobpeterson.alpaca.websocket.updates.UpdatesWebsocketInterface;

import java.util.concurrent.TimeUnit;

import static com.jessethouin.quant.conf.Config.CONFIG;

public class AlpacaApiServices {
    public static final AlpacaApiConfig ALPACA_API_CONFIG = AlpacaApiConfig.INSTANCE;
    public static final AlpacaAPI ALPACA_API;
    public static final AccountsApi ALPACA_ACCOUNT_API;
    public static final AccountActivitiesApi ALPACA_ACCOUNT_ACTIVITIES_API;
    public static final CryptoApi ALPACA_CRYPTO_API;
    public static final StockApi ALPACA_STOCK_API;
    public static final OrdersApi ALPACA_ORDERS_API;
    public static final PositionsApi ALPACA_POSITIONS_API;
    public static final UpdatesWebsocketInterface ALPACA_STREAMING_API;
    public static final CryptoMarketDataWebsocketInterface ALPACA_CRYPTO_STREAMING_API;
    public static final StockMarketDataWebsocketInterface ALPACA_STOCK_STREAMING_API;

    static {
        ALPACA_API = new AlpacaAPI(ALPACA_API_CONFIG.getKeyId(), ALPACA_API_CONFIG.getSecretKey(), ALPACA_API_CONFIG.getEndpointApiType(), ALPACA_API_CONFIG.getDataApiType());
        ALPACA_ACCOUNT_API = ALPACA_API.trader().accounts();
        ALPACA_ACCOUNT_ACTIVITIES_API = ALPACA_API.trader().accountActivities();
        ALPACA_CRYPTO_API = ALPACA_API.marketData().crypto();
        ALPACA_STOCK_API = ALPACA_API.marketData().stock();
        ALPACA_ORDERS_API = ALPACA_API.trader().orders();
        ALPACA_POSITIONS_API = ALPACA_API.trader().positions();
        ALPACA_STREAMING_API = ALPACA_API.updatesStream();
        ALPACA_CRYPTO_STREAMING_API = ALPACA_API.cryptoMarketDataStream();
        ALPACA_STOCK_STREAMING_API = ALPACA_API.stockMarketDataStream();

        if (!CONFIG.isBackTest()) {
            connectToCryptoStream();
            connectToStockStream();
            connectToUpdatesStream();
        }
    }

    private static void connectToCryptoStream() {
        ALPACA_CRYPTO_STREAMING_API.connect();
        ALPACA_CRYPTO_STREAMING_API.waitForAuthorization(5, TimeUnit.SECONDS);
        if (!ALPACA_CRYPTO_STREAMING_API.isValid()) {
            System.out.println("Websocket not valid!");
        }
    }

    private static void connectToStockStream() {
        ALPACA_STOCK_STREAMING_API.connect();
        ALPACA_STOCK_STREAMING_API.waitForAuthorization(5, TimeUnit.SECONDS);
        if (!ALPACA_STOCK_STREAMING_API.isValid()) {
            System.out.println("Websocket not valid!");
        }
    }

    private static void connectToUpdatesStream() {
        ALPACA_STREAMING_API.connect();
        ALPACA_STREAMING_API.waitForAuthorization(5, TimeUnit.SECONDS);
        if (!ALPACA_STREAMING_API.isValid()) {
            System.out.println("Websocket not valid!");
        }
    }
}
