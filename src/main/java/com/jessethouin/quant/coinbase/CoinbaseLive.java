package com.jessethouin.quant.coinbase;

import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CoinbaseLive {
    private static final Logger LOG = LogManager.getLogger(CoinbaseLive.class);
    static final CurrencyPair BTC_USDC = new CurrencyPair(Currency.BTC, Currency.USDC);
    private static final CompositeDisposable COMPOSITE_DISPOSABLE = new CompositeDisposable();

/*
    String url = "wss://ws-feed.pro.coinbase.com";
    String pub = "acbbb7db7e72d625191f00255722e977";
    String password = "bk2xd8fs3z";
    String secret = "sVIaCKqIFOtHR2TCk6G5lm+9OJ10nk1wwm0Ip8zjA8VvhB80XHJDF6fVanquGdcocZuCNrBctdR/3089E6N76w==";
**/
    public static void main(String[] args) {
        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(CoinbaseProStreamingExchange.class);
        exchange.connect(ProductSubscription.create().addOrderbook(BTC_USDC).build()).blockingAwait();

        Disposable subscription = exchange.getStreamingMarketDataService()
                .getOrderBook(BTC_USDC)
                .subscribe(
                        orderBook -> {
                            List<LimitOrder> asks = orderBook.getAsks();
                            asks.sort(Comparator.comparing(LimitOrder::getLimitPrice));
                            List<BigDecimal> askPrices = new ArrayList<>();
                            asks.forEach(l -> askPrices.add(l.getLimitPrice()));

                            List<LimitOrder> bids = orderBook.getBids();
                            bids.sort(Comparator.comparing(LimitOrder::getLimitPrice).reversed());
                            List<BigDecimal> bidPrices = new ArrayList<>();
                            bids.forEach(l -> bidPrices.add(l.getLimitPrice()));
                            LOG.info("spread: {} - {} - {}", askPrices.get(0), askPrices.get(0).subtract(bidPrices.get(0)), bidPrices.get(0));
                        },
                        throwable -> LOG.error("Error in orderBook subscription", throwable));

        COMPOSITE_DISPOSABLE.add(subscription);
    }

}
