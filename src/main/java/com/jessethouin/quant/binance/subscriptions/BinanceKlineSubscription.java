package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.binance.BinanceStreamProcessing.processMarketData;

import com.jessethouin.quant.broker.Fundamentals;
import io.reactivex.disposables.Disposable;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;

@Builder
public class BinanceKlineSubscription implements Disposable, Runnable {
    private static final Logger LOG = LogManager.getLogger(BinanceKlineSubscription.class);
    private final Fundamentals fundamentals;
    private ScheduledExecutorService executor;

    public Disposable subscribe() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this, 0L, 60L, TimeUnit.SECONDS);
        return this;
    }

    @Override
    public void run() {
        if (fundamentals == null)
            return;
        try {
            BinanceKline binanceKline = BINANCE_MARKET_DATA_SERVICE.lastKline(fundamentals.getCurrencyPair(), KlineInterval.m1);
            fundamentals.setPrice(binanceKline.getClosePrice());
            fundamentals.setTimestamp(new Date(binanceKline.getCloseTime()));
            processMarketData(fundamentals);
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
