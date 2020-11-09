package com.jessethouin.quant.binance;

import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.knowm.xchange.currency.CurrencyPair.BTC_USDT;

public class BinanceLive {
    private static final Logger LOG = LogManager.getLogger(BinanceLive.class);

    public static void main(String[] args) {
        ExchangeSpecification exSpec = new BinanceStreamingExchange().getDefaultExchangeSpecification();
        exSpec.setUserName("54704697");
        exSpec.setApiKey("M4gIEsmhsp5MjIkSZRapUUxScnZno56OHwJOvh1Bp3qIxW54FGCZnOxUYneNjVXB");
        exSpec.setSecretKey("tlAe5qFbA8oVDH0M085pYANzRD0EPHVteicsKk6rlKg1gEdC3j1lkSF3FMpd7jkO");
        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(exSpec);

        exchange.connect(ProductSubscription.create()
                .addOrderbook(BTC_USDT)
                .addOrders(BTC_USDT)
                .addTicker(BTC_USDT)
                .addTrades(BTC_USDT)
                .build()).blockingAwait();

        Disposable orderBookSub = exchange.getStreamingMarketDataService()
                .getOrderBook(BTC_USDT)
                .subscribe(
                        orderBook -> {
                            List<LimitOrder> asks = orderBook.getAsks();
                            asks.sort(Comparator.comparing(LimitOrder::getLimitPrice));
                            List<BigDecimal> askPrices = new ArrayList<>();
                            asks.forEach(l -> {
                                askPrices.add(l.getLimitPrice());
                            });

                            List<LimitOrder> bids = orderBook.getBids();
                            bids.sort(Comparator.comparing(LimitOrder::getLimitPrice).reversed());
                            List<BigDecimal> bidPrices = new ArrayList<>();
                            bids.forEach(l -> {
                                bidPrices.add(l.getLimitPrice());
                            });
                            LOG.info("spread: {} - {} - {}", askPrices.get(0), askPrices.get(0).subtract(bidPrices.get(0)), bidPrices.get(0));
                        },
                        throwable -> LOG.error("Error in orderBook subscription", throwable));

        Disposable tradeSub = exchange.getStreamingMarketDataService()
                .getTrades(BTC_USDT)
                .subscribe(trade -> LOG.info("Trade: {}", trade));

        Disposable orderSub = exchange.getStreamingTradeService()
                .getOrderChanges(BTC_USDT)
                .subscribe(order -> LOG.info("Order: {}", order));
    }
}
