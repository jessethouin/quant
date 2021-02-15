package com.jessethouin.quant.backtest;

import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(AbstractBacktest.class);
    public static final Config CONFIG = Config.INSTANCE;
    public static final List<BigDecimal> INTRADAY_PRICES = new ArrayList<>();

    public static void populateIntradayPrices() {
        LOG.info("Loading historic trades from DB, {} to {}.", CONFIG.getBacktestStart(), CONFIG.getBacktestEnd());
        updateBinanceTradeHistoryTable();
        INTRADAY_PRICES.clear();
        Database.getBinanceTradeHistory(CONFIG.getBacktestStart(), CONFIG.getBacktestEnd()).stream().map(BinanceTradeHistory::getP).forEach(INTRADAY_PRICES::add);
        LOG.info("Finished loading trades from DB");
    }

    public static void logMarketChange(BigDecimal endTicker, BigDecimal startTicker, Logger log) {
        BigDecimal change = (endTicker.divide(startTicker, 4, RoundingMode.HALF_DOWN)).subtract(BigDecimal.ONE).movePointRight(2);
        log.info("\n\tbegin: {}\n\tend:   {}\n\tdiff:  {} ({}%)", Util.formatFiat(startTicker), Util.formatFiat(endTicker), Util.formatFiat(endTicker.subtract(startTicker)), change);
    }

    private static void updateBinanceTradeHistoryTable() {
        Date latestBinanceTradeHistoryDate = Database.getLatestBinanceTradeHistoryDate();
        if (CONFIG.getBacktestEnd().after(latestBinanceTradeHistoryDate)) {
            LocalDateTime then  = latestBinanceTradeHistoryDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
            LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
            long minutes = Duration.between(then, now).toMinutes();
            CONFIG.setBacktestQty((int) minutes);
            BinanceCaptureHistory.doCapture(Date.from(now.toInstant(ZoneOffset.UTC)).getTime());
        }
    }
}
