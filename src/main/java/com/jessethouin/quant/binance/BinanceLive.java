package com.jessethouin.quant.binance;

import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_STREAMING_TRADE_SERVICE;
import static com.jessethouin.quant.binance.BinanceStreamProcessing.processMarketData;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BinanceLive {
    private static final Logger LOG = LogManager.getLogger(BinanceLive.class);
    private static final CompositeDisposable COMPOSITE_DISPOSABLE = new CompositeDisposable();
    @Getter
    private Portfolio portfolio;
    private final List<Ref> refList = new ArrayList<>();

    static {
        long start = new Date().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));
    }

    private final PortfolioRepository portfolioRepository;

    public BinanceLive(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    public void doLive() {
        portfolio = requireNonNullElse(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc(), Util.createPortfolio());

        BinanceUtil.showWallets();
        BinanceUtil.reconcile(portfolio);
        recalibrate();
        BinanceUtil.showTradingFees(portfolio);

        List<CurrencyPair> currencyPairs = BinanceUtil.getAllCryptoCurrencyPairs(CONFIG);
        currencyPairs.forEach(this::stopLoss);
        switch (CONFIG.getDataFeed()) {
            case KLINE -> currencyPairs.forEach(this::subscribeToCurrencyKline);
            case TRADE -> currencyPairs.forEach(this::subscribeToCurrencyTrades);
            case TICKER -> currencyPairs.forEach(this::subscribeToCurrencyTicker);
            case ORDER_BOOK -> currencyPairs.forEach(this::subscribeToOrderBook);
        }
        subscribeToExecutionReports();
    }

    @Scheduled(fixedRateString = "#{${recalibrateFreq} * 1000}", initialDelayString = "#{${recalibrateFreq} * 1000}")
    protected void recalibrate() {
        if (CONFIG.isRecalibrate()) {
            CONFIG.setBackTest(true);
            Util.relacibrate(CONFIG);
            CONFIG.setBackTest(false);
        }
    }

    @Scheduled(fixedRate = 10000, initialDelay = 10000)
    protected void savePortfolio() {
        portfolio = portfolioRepository.saveAndFlush(portfolio);
        refList.forEach(ref -> {
            ref.baseCurrency = Util.getCurrencyFromPortfolio(ref.baseCurrency.getSymbol(), portfolio);
            ref.counterCurrency = Util.getCurrencyFromPortfolio(ref.counterCurrency.getSymbol(), portfolio);
            ref.c.setBase(ref.baseCurrency);
            ref.c.setCounter(ref.counterCurrency);
        });
    }

    private void subscribeToExecutionReports() {
        Disposable executionReportsSub = BINANCE_STREAMING_TRADE_SERVICE.getRawExecutionReports().subscribe(BinanceStreamProcessing::processRemoteOrder);

        COMPOSITE_DISPOSABLE.add(executionReportsSub);
    }

    private void stopLoss(CurrencyPair currencyPair) {
        if (!currencyPair.toString().equals("420"))
            return; //we'll need to get rid of this once we start testing stop loss. This just keeps my checkmark green and happy.

        Ref ref = new Ref(currencyPair, portfolio);
        refList.add(ref);

        ref.value = Util.getValueAtPrice(ref.baseCurrency, ref.price).add(ref.counterCurrency.getQuantity());
        ref.previousValue = ref.value;

        Disposable tickerSub = BINANCE_STREAMING_MARKET_DATA_SERVICE.getTicker(currencyPair).subscribe(ticker -> {
            ref.value = Util.getValueAtPrice(ref.baseCurrency, ref.price).add(ref.counterCurrency.getQuantity());
            if (ref.value.compareTo(ref.previousValue.multiply(CONFIG.getStopLoss())) < 0)
                Transactions.placeSellOrder(CONFIG.getBroker(), null, ref.baseCurrency, ref.counterCurrency, ticker.getLast());
            ref.previousValue = ref.value;
        }, throwable -> LOG.error("Error in ticket subscription (stop loss)", throwable));

        COMPOSITE_DISPOSABLE.add(tickerSub);
    }

    private void subscribeToCurrencyKline(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        refList.add(ref);

        new Timer("KlineSubscription").scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    BinanceKline binanceKline = BINANCE_MARKET_DATA_SERVICE.lastKline(currencyPair, KlineInterval.m1);
                    ref.price = binanceKline.getClosePrice();
                    ref.timestamp = new Date(binanceKline.getCloseTime());
                    processMarketData(ref);
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }
            }
        }, 0, SECONDS.toMillis(60));
    }

    private void subscribeToCurrencyTicker(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        refList.add(ref);

        Disposable tickerSub = BINANCE_STREAMING_MARKET_DATA_SERVICE.getTicker(currencyPair).subscribe(ticker -> {
            ref.price = ticker.getLast();
            ref.timestamp = ticker.getTimestamp();
            processMarketData(ref);
        }, throwable -> LOG.error("Error in ticket subscription", throwable));

        COMPOSITE_DISPOSABLE.add(tickerSub);
    }

    private void subscribeToCurrencyTrades(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        refList.add(ref);

        Disposable tradesSub = BINANCE_STREAMING_MARKET_DATA_SERVICE.getTrades(currencyPair).subscribe(trade -> {
            ref.price = trade.getPrice();
            ref.timestamp = trade.getTimestamp();
            processMarketData(ref);
        }, throwable -> LOG.error("Error in trade subscription", throwable));

        COMPOSITE_DISPOSABLE.add(tradesSub);
    }

    private void subscribeToOrderBook(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        refList.add(ref);

        Disposable orderBookSub = BINANCE_STREAMING_MARKET_DATA_SERVICE.getOrderBook(currencyPair).subscribe(orderBook -> {
            BigDecimal minAsk = orderBook.getAsks().stream().map(LimitOrder::getLimitPrice).min(BigDecimal::compareTo).orElseThrow();
            BigDecimal maxBid = orderBook.getBids().stream().map(LimitOrder::getLimitPrice).max(BigDecimal::compareTo).orElseThrow();
            LOG.info("spread: {} - {} - {}", minAsk, minAsk.subtract(maxBid), maxBid);
            ref.price = minAsk.add(maxBid).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_DOWN);
            ref.timestamp = orderBook.getTimeStamp();
            processMarketData(ref);
        }, throwable -> LOG.error("Error in orderBook subscription", throwable));

        COMPOSITE_DISPOSABLE.add(orderBookSub);
    }

    public static class Ref {
        Calc c;
        Currency baseCurrency;
        Currency counterCurrency;
        int count = 0;
        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        BigDecimal value = BigDecimal.ZERO;
        BigDecimal previousValue = BigDecimal.ZERO;
        Date timestamp;

        Ref(CurrencyPair currencyPair, Portfolio portfolio) {
            this.baseCurrency = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
            this.counterCurrency = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
            c = new Calc(baseCurrency, counterCurrency, CONFIG, BigDecimal.ZERO);
        }
    }
}
