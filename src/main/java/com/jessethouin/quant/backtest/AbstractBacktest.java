package com.jessethouin.quant.backtest;

import com.jessethouin.quant.alpaca.AlpacaCaptureHistory;
import com.jessethouin.quant.backtest.beans.repos.BacktestParameterResultsRepository;
import com.jessethouin.quant.beans.TradeHistory;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.broker.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

@Component
public abstract class AbstractBacktest {

    private static final Logger LOG = LogManager.getLogger(AbstractBacktest.class);
    public static final List<BigDecimal> INTRADAY_PRICES = new ArrayList<>();
    static TradeHistoryRepository tradeHistoryRepository;
    static BacktestParameterResultsRepository backtestParameterResultsRepository;
    static BinanceCaptureHistory binanceCaptureHistory;
    static AlpacaCaptureHistory alpacaCaptureHistory;

    public AbstractBacktest(TradeHistoryRepository tradeHistoryRepository, BacktestParameterResultsRepository backtestParameterResultsRepository, BinanceCaptureHistory binanceCaptureHistory, AlpacaCaptureHistory alpacaCaptureHistory) {
        AbstractBacktest.tradeHistoryRepository = tradeHistoryRepository;
        AbstractBacktest.backtestParameterResultsRepository = backtestParameterResultsRepository;
        AbstractBacktest.binanceCaptureHistory = binanceCaptureHistory;
        AbstractBacktest.alpacaCaptureHistory = alpacaCaptureHistory;
    }

    public void populateIntradayPrices() {
        LOG.info("Loading historic trades from DB, {} to {}.", CONFIG.getBacktestStart(), CONFIG.getBacktestEnd());
        updateTradeHistoryTable();
        INTRADAY_PRICES.clear();
        tradeHistoryRepository.getTradeHistoriesByTimestampBetween(CONFIG.getBacktestStart(), CONFIG.getBacktestEnd()).stream().map(TradeHistory::getP).forEach(INTRADAY_PRICES::add);
        LOG.info("Finished loading trades from DB");
    }

    public void logMarketChange(BigDecimal endTicker, BigDecimal startTicker, Logger log) {
        BigDecimal change = (endTicker.divide(startTicker, 4, RoundingMode.HALF_DOWN)).subtract(BigDecimal.ONE).movePointRight(2);
        log.info("\n\tbegin: {}\n\tend:   {}\n\tdiff:  {} ({}%)", Util.formatFiat(startTicker), Util.formatFiat(endTicker), Util.formatFiat(endTicker.subtract(startTicker)), change);
    }

    private void updateTradeHistoryTable() {
        LocalDateTime start = LocalDateTime.ofInstant(CONFIG.getBacktestStart().toInstant(), ZoneId.systemDefault());
        LocalDateTime end = LocalDateTime.ofInstant(CONFIG.getBacktestEnd().toInstant(), ZoneId.systemDefault());
        long diff = 0L;

        switch (CONFIG.getDataFeed()) {
            case TICKER -> diff = SECONDS.between(start, end);
            case KLINE, BAR -> diff = MINUTES.between(start, end);
            default -> tradeHistoryRepository.deleteAll();
        }

        final Long bthCount = tradeHistoryRepository.countTradeHistoriesByTimestampBetween(CONFIG.getBacktestStart(), CONFIG.getBacktestEnd());
        if (bthCount + 1 != diff) tradeHistoryRepository.deleteAll();
        else return;

        switch (CONFIG.getBroker()) {
            case ALPACA, ALPACA_SECURITY_TEST, ALPACA_CRYPTO_TEST: alpacaCaptureHistory.doCapture();
            case BINANCE, BINANCE_TEST: binanceCaptureHistory.doCapture();
        }
    }
}
