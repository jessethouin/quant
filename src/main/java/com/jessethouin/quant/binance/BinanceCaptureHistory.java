package com.jessethouin.quant.binance;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.repos.BinanceTradeHistoryRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.marketdata.BinanceAggTrades;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.currency.CurrencyPair;
import org.springframework.stereotype.Component;

@Component
public class BinanceCaptureHistory {
    private static final Logger LOG = LogManager.getLogger(BinanceCaptureHistory.class);
    static BinanceTradeHistoryRepository binanceTradeHistoryRepository;

    public BinanceCaptureHistory(BinanceTradeHistoryRepository binanceTradeHistoryRepository) {
        BinanceCaptureHistory.binanceTradeHistoryRepository = binanceTradeHistoryRepository;
    }

    public void doCapture() {
        CurrencyPair currencyPair = CurrencyPair.BTC_USDT;
        try {
            List<BinanceTradeHistory> bs = new ArrayList<>(); // bs is plural for BinanceTradeHistor-ies - not bullshit. I suppose I could have used bths. But that just looks like baths - which I may take if this software sucks.

            switch(CONFIG.getDataFeed()) {
                case KLINE -> {
                    long start = CONFIG.getBacktestStart().getTime();
                    long end = CONFIG.getBacktestEnd().getTime();
                    long qty = end - start; //milliseonds between start and end dates/times
                    for (long i = qty; i > -1; i -= MINUTES.toMillis(500)) {
                        long e = start + Math.min(MINUTES.toMillis(500), i);
                        BINANCE_MARKET_DATA_SERVICE.klines(currencyPair, KlineInterval.m1, 500, start, e).forEach(binanceKline -> {
                            BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder().timestamp(new Date(binanceKline.getCloseTime())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(binanceKline.getClosePrice()).build();
                            bs.add(binanceTradeHistory);
                        });
                        start = e + 1;
                    }
                }
                case TRADE -> {
                    final BinanceAggTrades aggTrade = BINANCE_MARKET_DATA_SERVICE.aggTrades(currencyPair, null, CONFIG.getBacktestStart().getTime(), CONFIG.getBacktestEnd().getTime(), 1).get(0);
                    AtomicLong tradeId = new AtomicLong(aggTrade.aggregateTradeId);
                    AtomicLong tradeTime = new AtomicLong(aggTrade.timestamp);
                    while (tradeTime.get() < CONFIG.getBacktestEnd().getTime()) {
                        BINANCE_MARKET_DATA_SERVICE.aggTrades(currencyPair, tradeId.get(), null, null, 1000).forEach(binanceAggTrades -> {
                            BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder().timestamp(binanceAggTrades.getTimestamp()).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(binanceAggTrades.price).build();
                            tradeId.set(binanceTradeHistory.getTradeId());
                            tradeTime.set(binanceTradeHistory.getTimestamp().getTime());
                            bs.add(binanceTradeHistory);
                        });
                    }
                }
                case TICKER -> {
                    final List<BinanceAggTrades> aggTrades = BINANCE_MARKET_DATA_SERVICE.aggTrades(currencyPair, null, CONFIG.getBacktestStart().getTime(), CONFIG.getBacktestStart().getTime() + 500, 1000);
                    final BinanceAggTrades aggTrade = aggTrades.get(0);
                    final AtomicLong aggregateTradeId = new AtomicLong(aggTrade.aggregateTradeId);
                    final AtomicLong tradeTime = new AtomicLong(aggTrade.timestamp);
                    final AtomicLong tickerTime = new AtomicLong(aggTrade.timestamp + (1000L - (aggTrade.timestamp % 1000L)));
                    final AtomicReference<BinanceAggTrades> previousAggTrades = new AtomicReference<>();
                    while (tickerTime.get() < CONFIG.getBacktestEnd().getTime()) {
                        BINANCE_MARKET_DATA_SERVICE.aggTrades(currencyPair, aggregateTradeId.get(), null, null, 1000).forEach(binanceAggTrades -> {
                            System.out.print(new Date(tickerTime.get()).toLocaleString() + "\r");
                            if (binanceAggTrades.timestamp < tickerTime.get()) {
                                previousAggTrades.set(binanceAggTrades);
                                return;
                            }
                            final BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder().timestamp(previousAggTrades.get().getTimestamp()).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(previousAggTrades.get().price).build();
                            bs.add(binanceTradeHistory);
                            aggregateTradeId.set(previousAggTrades.get().aggregateTradeId);
                            tradeTime.set(binanceTradeHistory.getTimestamp().getTime());
                            tickerTime.set(tickerTime.get() + 1000L);
                        });
                    }
                }
            }

            binanceTradeHistoryRepository.saveAll(bs);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}
