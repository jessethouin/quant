package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BinanceCaptureHistory {
    private static final Logger LOG = LogManager.getLogger(BinanceCaptureHistory.class);
    private static final Config config = Config.INSTANCE;
    private static final BinanceStreamingExchange BINANCE_STREAMING_EXCHANGE;

    static {
        ExchangeSpecification strExSpec = new BinanceStreamingExchange().getDefaultExchangeSpecification();
        strExSpec.setUserName("54704697");
        strExSpec.setApiKey("M4gIEsmhsp5MjIkSZRapUUxScnZno56OHwJOvh1Bp3qIxW54FGCZnOxUYneNjVXB");
        strExSpec.setSecretKey("tlAe5qFbA8oVDH0M085pYANzRD0EPHVteicsKk6rlKg1gEdC3j1lkSF3FMpd7jkO");
        BINANCE_STREAMING_EXCHANGE = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(strExSpec);
    }

    public static void main(String[] args) {
        CurrencyPair currencyPair = CurrencyPair.BTC_USDT;

        var ref = new Object() {
            final List<BigDecimal> intradayPrices = new ArrayList<>();
            int count = 0;
            BigDecimal shortMAValue;
            BigDecimal longMAValue;
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal previousShortMAValue = BigDecimal.ZERO;
            BigDecimal previousLongMAValue = BigDecimal.ZERO;
        };

        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create().addAll(currencyPair);
        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();

/*
        Disposable tradeSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getTrades(currencyPair)
                .subscribe(trade -> {
                    ref.price = trade.getPrice();
                    if (ref.count < config.getLongLookback()) ref.intradayPrices.add(ref.price);

                    ref.shortMAValue = Util.getMA(ref.intradayPrices, ref.previousShortMAValue, ref.count, config.getShortLookback(), ref.price);
                    ref.longMAValue = Util.getMA(ref.intradayPrices, ref.previousLongMAValue, ref.count, config.getLongLookback(), ref.price);
                    BinanceTradeHistory binanceTradeHistory = new BinanceTradeHistory.Builder().setTimestamp(new Date()).setMa1(ref.shortMAValue).setMa2(ref.longMAValue).setL(BigDecimal.ZERO).setH(BigDecimal.ZERO).setP(ref.price).build();
                    Database.persistTradeHistory(binanceTradeHistory);
                    LOG.info("ma1 {} ma2 {} p {}", ref.shortMAValue, ref.longMAValue, ref.price);

                    ref.previousShortMAValue = ref.shortMAValue;
                    ref.previousLongMAValue = ref.longMAValue;
                    ref.count++;
                });
*/

        Disposable tickerSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getTicker(currencyPair)
                .subscribe(t -> {
                    ref.price = t.getLast();
                    if (ref.count < config.getLongLookback()) ref.intradayPrices.add(ref.price);

                    ref.shortMAValue = Util.getMA(ref.intradayPrices, ref.previousShortMAValue, ref.count, config.getShortLookback(), ref.price);
                    ref.longMAValue = Util.getMA(ref.intradayPrices, ref.previousLongMAValue, ref.count, config.getLongLookback(), ref.price);
                    BinanceTradeHistory binanceTradeHistory = new BinanceTradeHistory.Builder().setTimestamp(t.getTimestamp()).setMa1(ref.shortMAValue).setMa2(ref.longMAValue).setL(BigDecimal.ZERO).setH(BigDecimal.ZERO).setP(ref.price).build();
                    Database.persistTradeHistory(binanceTradeHistory);
                    LOG.info("ma1 {} ma2 {} p {}", ref.shortMAValue, ref.longMAValue, ref.price);

                    ref.previousShortMAValue = ref.shortMAValue;
                    ref.previousLongMAValue = ref.longMAValue;
                    ref.count++;
                });

    }

}
