package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.binance.BinanceStreamProcessing.processMarketData;

import com.jessethouin.quant.broker.Fundamentals;
import io.reactivex.disposables.Disposable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.dto.trade.LimitOrder;

@Builder
public class BinanceOrderBookSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceOrderBookSubscription.class);
    private final Fundamentals fundamentals;

    public Disposable subscribe() {
        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getOrderBook(fundamentals.getCurrencyPair()).subscribe(orderBook -> {
            BigDecimal minAsk = orderBook.getAsks().stream().map(LimitOrder::getLimitPrice).min(BigDecimal::compareTo).orElseThrow();
            BigDecimal maxBid = orderBook.getBids().stream().map(LimitOrder::getLimitPrice).max(BigDecimal::compareTo).orElseThrow();
            LOG.info("spread: {} - {} - {}", minAsk, minAsk.subtract(maxBid), maxBid);
            fundamentals.setPrice(minAsk.add(maxBid).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_DOWN));
            fundamentals.setTimestamp(orderBook.getTimeStamp());
            processMarketData(fundamentals);
        }, throwable -> LOG.error("Error in orderBook subscription", throwable));
    }
}
