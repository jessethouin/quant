package com.jessethouin.quant.binance.subscriptions;

import com.jessethouin.quant.broker.Fundamental;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.jessethouin.quant.binance.BinanceStreamProcessor.processMarketData;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;

@Builder
public class BinanceOrderBookSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceOrderBookSubscription.class);
    private final Fundamental fundamental;

    public Disposable subscribe() {
        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getOrderBook(fundamental.getCurrencyPair()).subscribe(orderBook -> {
            BigDecimal minAsk = orderBook.getAsks().stream().map(LimitOrder::getLimitPrice).min(BigDecimal::compareTo).orElseThrow();
            BigDecimal maxBid = orderBook.getBids().stream().map(LimitOrder::getLimitPrice).max(BigDecimal::compareTo).orElseThrow();
            LOG.info("spread: {} - {} - {}", minAsk, minAsk.subtract(maxBid), maxBid);
            fundamental.setPrice(minAsk.add(maxBid).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_DOWN));
            fundamental.setTimestamp(orderBook.getTimeStamp());
            processMarketData(fundamental);
        }, throwable -> LOG.error("Error in orderBook subscription", throwable));
    }
}
