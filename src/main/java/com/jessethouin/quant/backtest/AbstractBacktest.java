package com.jessethouin.quant.backtest;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.jessethouin.quant.backtest.beans.repos.BacktestParameterResultsRepository;
import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.repos.BinanceTradeHistoryRepository;
import com.jessethouin.quant.broker.Util;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractBacktest {

    private static final Logger LOG = LogManager.getLogger(AbstractBacktest.class);
    public static final List<BigDecimal> INTRADAY_PRICES = new ArrayList<>();
    static BinanceTradeHistoryRepository binanceTradeHistoryRepository;
    static BacktestParameterResultsRepository backtestParameterResultsRepository;
    static BinanceCaptureHistory binanceCaptureHistory;

    public AbstractBacktest(BinanceTradeHistoryRepository binanceTradeHistoryRepository, BacktestParameterResultsRepository backtestParameterResultsRepository, BinanceCaptureHistory binanceCaptureHistory) {
        AbstractBacktest.binanceTradeHistoryRepository = binanceTradeHistoryRepository;
        AbstractBacktest.backtestParameterResultsRepository = backtestParameterResultsRepository;
        AbstractBacktest.binanceCaptureHistory = binanceCaptureHistory;
    }

    public void populateIntradayPrices() {
        LOG.info("Loading historic trades from DB, {} to {}.", CONFIG.getBacktestStart(), CONFIG.getBacktestEnd());
        updateBinanceTradeHistoryTable();
        INTRADAY_PRICES.clear();
        binanceTradeHistoryRepository.getBinanceTradeHistoriesByTimestampBetween(CONFIG.getBacktestStart(), CONFIG.getBacktestEnd()).stream().map(BinanceTradeHistory::getP).forEach(INTRADAY_PRICES::add);
        LOG.info("Finished loading trades from DB");
    }

    public void logMarketChange(BigDecimal endTicker, BigDecimal startTicker, Logger log) {
        BigDecimal change = (endTicker.divide(startTicker, 4, RoundingMode.HALF_DOWN)).subtract(BigDecimal.ONE).movePointRight(2);
        log.info("\n\tbegin: {}\n\tend:   {}\n\tdiff:  {} ({}%)", Util.formatFiat(startTicker), Util.formatFiat(endTicker), Util.formatFiat(endTicker.subtract(startTicker)), change);
    }

    private void updateBinanceTradeHistoryTable() {
        LocalDateTime start = LocalDateTime.ofInstant(CONFIG.getBacktestStart().toInstant(), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(CONFIG.getBacktestEnd().toInstant(), ZoneId.systemDefault());
        long diff = 0L;

        switch (CONFIG.getDataFeed()) {
            case TICKER -> diff = SECONDS.between(start, end);
            case KLINE -> diff = MINUTES.between(start, end);
            default -> binanceTradeHistoryRepository.deleteAll();
        }

        final Long bthCount = binanceTradeHistoryRepository.countBinanceTradeHistoriesByTimestampBetween(CONFIG.getBacktestStart(), CONFIG.getBacktestEnd());
        if (bthCount + 1 != diff) binanceTradeHistoryRepository.deleteAll();
        else return;

        binanceCaptureHistory.doCapture();
    }
}
