package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;

public class BinanceCaptureHistory {
    private static final Logger LOG = LogManager.getLogger(BinanceCaptureHistory.class);
    private static final Config config = Config.INSTANCE;

    static {
        ExchangeSpecification strExSpec = new BinanceStreamingExchange().getDefaultExchangeSpecification();
        strExSpec.setUserName("54704697");
        strExSpec.setApiKey("M4gIEsmhsp5MjIkSZRapUUxScnZno56OHwJOvh1Bp3qIxW54FGCZnOxUYneNjVXB");
        strExSpec.setSecretKey("tlAe5qFbA8oVDH0M085pYANzRD0EPHVteicsKk6rlKg1gEdC3j1lkSF3FMpd7jkO");
    }

    public static void main(String[] args) {
        CurrencyPair currencyPair = CurrencyPair.BTC_USDT;

        try {
            BinanceMarketDataService marketDataService = (BinanceMarketDataService) BinanceLive.INSTANCE.getBinanceExchange().getMarketDataService();
            long time = LocalDateTime.parse("2020-06-01T00:00:00").atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();

            Session session = Database.getSession();
            session.beginTransaction();

            for (int i = config.getBacktestQty(); i > -1; i = i - 500) {
                marketDataService.klines(currencyPair, KlineInterval.m1, 500, time - MINUTES.toMillis(i), time - MINUTES.toMillis(Math.max(i - 500, 0))).forEach(binanceKline -> {
                    BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder().timestamp(new Date(binanceKline.getCloseTime())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(binanceKline.getClosePrice()).build();
                    session.persist(binanceTradeHistory);
                });
                LOG.info(i);
            }

            session.getTransaction().commit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
