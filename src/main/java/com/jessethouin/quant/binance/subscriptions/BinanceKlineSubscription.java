package com.jessethouin.quant.binance.subscriptions;

import com.jessethouin.quant.broker.Fundamental;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.jessethouin.quant.binance.BinanceStreamProcessor.processMarketData;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_MARKET_DATA_SERVICE;

@Builder
public class BinanceKlineSubscription implements Disposable, Runnable {
    private static final Logger LOG = LogManager.getLogger(BinanceKlineSubscription.class);
    private final Fundamental fundamental;
    private ScheduledExecutorService executor;

    public Disposable subscribe() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this, 0L, 60L, TimeUnit.SECONDS);
        return this;
    }

    @Override
    public void run() {
        if (fundamental == null)
            return;
        try {
            BinanceKline binanceKline = BINANCE_MARKET_DATA_SERVICE.lastKline(fundamental.getCurrencyPair(), KlineInterval.m1);
            fundamental.setPrice(binanceKline.getClose());
            fundamental.setTimestamp(new Date(binanceKline.getCloseTime()));
            processMarketData(fundamental);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            boolean executed = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            if (!executed)
                LOG.warn("Binance Kline Subscription failed to shut down after ...* checks watch *... 292 years. That's a tad too long.");
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    @Override
    public boolean isDisposed() {
        return executor.isShutdown();
    }
}
