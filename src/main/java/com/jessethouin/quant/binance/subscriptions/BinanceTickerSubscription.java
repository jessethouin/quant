package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.binance.BinanceStreamProcessing.processMarketData;

import com.jessethouin.quant.broker.Fundamentals;
import io.reactivex.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class BinanceTickerSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceTickerSubscription.class);
    private final Fundamentals fundamentals;

    public Disposable subscribe() {
        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getTicker(fundamentals.getCurrencyPair()).subscribe(ticker -> {
            fundamentals.setPrice(ticker.getLast());
            fundamentals.setTimestamp(ticker.getTimestamp());
            processMarketData(fundamentals);
        }, throwable -> LOG.error("Error in ticket subscription", throwable));
    }
}
