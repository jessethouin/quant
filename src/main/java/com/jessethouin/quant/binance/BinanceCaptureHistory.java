package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;

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


        try {
            BinanceLive.Ref ref = new BinanceLive.Ref(currencyPair, Util.createPortfolio());
            Date start = new Date();

            BinanceMarketDataService marketDataService = (BinanceMarketDataService) BinanceLive.INSTANCE.getBinanceExchange().getMarketDataService();
            marketDataService.klines(currencyPair, KlineInterval.m1, 500, start.getTime() - MINUTES.toMillis(1440), start.getTime() - MINUTES.toMillis(1000)).stream().map(BinanceKline::getClosePrice).forEach(ref.intradayPrices::add);
            marketDataService.klines(currencyPair, KlineInterval.m1, 500, start.getTime() - MINUTES.toMillis(1000), start.getTime() + MINUTES.toMillis(500)).stream().map(BinanceKline::getClosePrice).forEach(ref.intradayPrices::add);
            marketDataService.klines(currencyPair, KlineInterval.m1, 500, start.getTime() - MINUTES.toMillis(500), start.getTime()).stream().map(BinanceKline::getClosePrice).forEach(ref.intradayPrices::add);
            ref.intradayPrices.forEach(bigDecimal -> {
                ref.price = bigDecimal;
                ref.timestamp = new Date(start.getTime() - MINUTES.toMillis(1440) + MINUTES.toMillis(ref.count));
                doTheThing(ref);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

/*
        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create().addAll(currencyPair);
        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();

        Disposable tradeSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getTrades(currencyPair)
                .subscribe(t -> {
                    ref.price = t.getPrice();
                    ref.timestamp = t.getTimestamp();
                    doTheThing(ref);
                });

        Disposable tickerSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getTicker(currencyPair)
                .subscribe(t -> {
                    ref.price = t.getLast();
                    ref.timestamp = t.getTimestamp();
                    doTheThing(ref);
                });
*/

    }

    private static void doTheThing(BinanceLive.Ref ref) {
        if (ref.count < config.getLongLookback() && ref.count > ref.intradayPrices.size()) ref.intradayPrices.add(ref.price);

        ref.shortMAValue = Util.getMA(ref.intradayPrices, ref.previousShortMAValue, ref.count, config.getShortLookback(), ref.price);
        ref.longMAValue = Util.getMA(ref.intradayPrices, ref.previousLongMAValue, ref.count, config.getLongLookback(), ref.price);
        BinanceTradeHistory binanceTradeHistory = new BinanceTradeHistory.Builder().setTimestamp(ref.timestamp).setMa1(ref.shortMAValue).setMa2(ref.longMAValue).setL(BigDecimal.ZERO).setH(BigDecimal.ZERO).setP(ref.price).build();
        Database.persistTradeHistory(binanceTradeHistory);
        LOG.trace("ma1 {} ma2 {} p {}", ref.shortMAValue, ref.longMAValue, ref.price);

        ref.previousShortMAValue = ref.shortMAValue;
        ref.previousLongMAValue = ref.longMAValue;
        ref.count++;
    }

}
