package com.jessethouin.quant.binance.subscriptions;

import com.jessethouin.quant.broker.Fundamental;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.jessethouin.quant.binance.BinanceStreamProcessor.processMarketData;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;

@Builder
public class BinanceTradeSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceTradeSubscription.class);
    private final Fundamental fundamental;

    public Disposable subscribe() {
        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getTrades(fundamental.getCurrencyPair()).subscribe(trade -> {
            fundamental.setPrice(trade.getPrice());
            fundamental.setTimestamp(trade.getTimestamp());
            processMarketData(fundamental);
        }, throwable -> LOG.error("Error in trade subscription", throwable));
    }
}
