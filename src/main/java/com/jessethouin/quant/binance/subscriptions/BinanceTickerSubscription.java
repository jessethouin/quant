package com.jessethouin.quant.binance.subscriptions;

import com.jessethouin.quant.broker.Fundamental;
import io.reactivex.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.jessethouin.quant.binance.BinanceStreamProcessor.processMarketData;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;

@Builder
public class BinanceTickerSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceTickerSubscription.class);
    private final Fundamental fundamental;

    public Disposable subscribe() {
        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getTicker(fundamental.getCurrencyPair()).subscribe(ticker -> {
            fundamental.setPrice(ticker.getLast());
            fundamental.setTimestamp(ticker.getTimestamp());
            processMarketData(fundamental);
        }, throwable -> LOG.error("Error in ticket subscription", throwable));
    }
}
