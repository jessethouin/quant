package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import static java.util.concurrent.TimeUnit.MINUTES;

public class BinanceCaptureHistory {
    private static final Logger LOG = LogManager.getLogger(BinanceCaptureHistory.class);
    private static final Config CONFIG = Config.INSTANCE;

    public static void main(String[] args) {
        doCapture(new Date().getTime());
    }

    public static void doCapture(long now) {
        CurrencyPair currencyPair = CurrencyPair.BTC_USDT;

        try {
            BinanceMarketDataService marketDataService = (BinanceMarketDataService) BinanceLive.INSTANCE.getBinanceExchange().getMarketDataService();

            Session session = Database.getSession();
            session.beginTransaction();

            for (int i = CONFIG.getBacktestQty(); i > -1; i -= 500) {
                marketDataService.klines(currencyPair, KlineInterval.m1, 500, now - MINUTES.toMillis(i), now - MINUTES.toMillis(Math.max(i - 500, 0))).forEach(binanceKline -> {
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
