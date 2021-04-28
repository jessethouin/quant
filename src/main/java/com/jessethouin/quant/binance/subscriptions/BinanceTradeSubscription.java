package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.binance.BinanceStreamProcessing.processMarketData;

import com.jessethouin.quant.broker.Fundamentals;
import io.reactivex.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class BinanceTradeSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceTradeSubscription.class);
    private final Fundamentals fundamentals;

    public Disposable subscribe() {
        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getTrades(fundamentals.getCurrencyPair()).subscribe(trade -> {
            fundamentals.setPrice(trade.getPrice());
            fundamentals.setTimestamp(trade.getTimestamp());
            processMarketData(fundamentals);
        }, throwable -> LOG.error("Error in trade subscription", throwable));
    }
}
