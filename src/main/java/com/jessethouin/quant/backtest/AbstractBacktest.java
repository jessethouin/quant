package com.jessethouin.quant.backtest;

import com.jessethouin.quant.backtest.beans.repos.BacktestParameterResultsRepository;
import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.repos.BinanceTradeHistoryRepository;
import com.jessethouin.quant.broker.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.conf.Config.CONFIG;

@Component
public abstract class AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(AbstractBacktest.class);
    public static final List<BigDecimal> INTRADAY_PRICES = new ArrayList<>();
    static BinanceTradeHistoryRepository binanceTradeHistoryRepository;
    static BacktestParameterResultsRepository backtestParameterResultsRepository;

    public AbstractBacktest(BinanceTradeHistoryRepository binanceTradeHistoryRepository, BacktestParameterResultsRepository backtestParameterResultsRepository) {
        AbstractBacktest.binanceTradeHistoryRepository = binanceTradeHistoryRepository;
        AbstractBacktest.backtestParameterResultsRepository = backtestParameterResultsRepository;
    }

    public static void populateIntradayPrices() {
        LOG.info("Loading historic trades from DB, {} to {}.", CONFIG.getBacktestStart(), CONFIG.getBacktestEnd());
        updateBinanceTradeHistoryTable();
        INTRADAY_PRICES.clear();
        binanceTradeHistoryRepository.getBinanceTradeHistoriesByTimestampBetween(CONFIG.getBacktestStart(), CONFIG.getBacktestEnd()).stream().map(BinanceTradeHistory::getP).forEach(INTRADAY_PRICES::add);
        LOG.info("Finished loading trades from DB");
    }

    public static void logMarketChange(BigDecimal endTicker, BigDecimal startTicker, Logger log) {
        BigDecimal change = (endTicker.divide(startTicker, 4, RoundingMode.HALF_DOWN)).subtract(BigDecimal.ONE).movePointRight(2);
        log.info("\n\tbegin: {}\n\tend:   {}\n\tdiff:  {} ({}%)", Util.formatFiat(startTicker), Util.formatFiat(endTicker), Util.formatFiat(endTicker.subtract(startTicker)), change);
    }

    private static void updateBinanceTradeHistoryTable() {
        Date latestBinanceTradeHistoryDate = binanceTradeHistoryRepository.getMaxTimestamp();
        if (CONFIG.getBacktestEnd().after(latestBinanceTradeHistoryDate)) {
            LocalDateTime then  = latestBinanceTradeHistoryDate.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
            LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
            long minutes = Duration.between(then, now).toMinutes();
            CONFIG.setBacktestQty((int) minutes);
            BinanceCaptureHistory.doCapture(Date.from(now.toInstant(ZoneOffset.UTC)).getTime());
        }
    }
}
