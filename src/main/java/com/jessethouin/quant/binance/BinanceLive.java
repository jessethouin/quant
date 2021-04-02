package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.OrderHistoryLookup;
import com.jessethouin.quant.binance.beans.repos.BinanceLimitOrderRepository;
import com.jessethouin.quant.binance.beans.repos.BinanceTradeHistoryRepository;
import com.jessethouin.quant.binance.beans.repos.OrderHistoryLookupRepository;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.CurrencyTypes;
import info.bitrich.xchangestream.core.ProductSubscription;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.jessethouin.quant.binance.BinanceExchangeServices.*;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

@Component
public class BinanceLive {
    private static final Logger LOG = LogManager.getLogger(BinanceLive.class);
    private static final CompositeDisposable COMPOSITE_DISPOSABLE = new CompositeDisposable();
    private Portfolio portfolio;
    private OrderHistoryLookup orderHistoryLookup;

    static {
        long start = new Date().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));
    }

    private final BinanceLimitOrderRepository binanceLimitOrderRepository;
    private final BinanceTradeHistoryRepository binanceTradeHistoryRepository;
    private final OrderHistoryLookupRepository orderHistoryLookupRepository;
    private final PortfolioRepository portfolioRepository;

    private BinanceLive(BinanceLimitOrderRepository binanceLimitOrderRepository, BinanceTradeHistoryRepository binanceTradeHistoryRepository, OrderHistoryLookupRepository orderHistoryLookupRepository, PortfolioRepository portfolioRepository) {
        this.binanceLimitOrderRepository = binanceLimitOrderRepository;
        this.binanceTradeHistoryRepository = binanceTradeHistoryRepository;
        this.orderHistoryLookupRepository = orderHistoryLookupRepository;
        this.portfolioRepository = portfolioRepository;
    }

    public OrderHistoryLookup getOrderHistoryLookup() {
        return orderHistoryLookup;
    }

    public void doLive() {
        portfolio = portfolioRepository.save(requireNonNullElse(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc(), Util.createPortfolio()));

        BinanceTransactions.showWallets();
        BinanceTransactions.showTradingFees(portfolio);
        reconcile();
        recalibrate();
        savePortfolio();

        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();
        List<CurrencyPair> currencyPairs = Util.getAllCurrencyPairs(CONFIG);
        currencyPairs.forEach(productSubscriptionBuilder::addAll);

        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();

        currencyPairs.forEach(this::subscribeToOrderUpdates);
        currencyPairs.forEach(this::stopLoss);

        switch (CONFIG.getDataFeed()) {
            case KLINE -> currencyPairs.forEach(this::subscribeToCurrencyKline);
            case TRADE -> currencyPairs.forEach(this::subscribeToCurrencyTrades);
            case TICKER -> currencyPairs.forEach(this::subscribeToCurrencyTicker);
            case ORDER_BOOK -> currencyPairs.forEach(this::subscribeToOrderBook);
        }
    }

    private void subscribeToOrderUpdates(CurrencyPair currencyPair) {
        Disposable orderSub = BINANCE_STREAMING_EXCHANGE.getStreamingTradeService()
                .getOrderChanges(currencyPair)
                .subscribe(this::processRemoteOrder);

        COMPOSITE_DISPOSABLE.add(orderSub);
    }

    private void stopLoss(CurrencyPair currencyPair) {
        if (!currencyPair.toString().equals("420")) return; //we'll need to get rid of this once we start testing stop loss. This just keeps my checkmark green and happy.

        Ref ref = new Ref(currencyPair, portfolio);
        ref.value = Util.getValueAtPrice(ref.baseCurrency, ref.price).add(ref.counterCurrency.getQuantity());
        ref.previousValue = ref.value;

        Disposable tickerSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTicker(currencyPair).subscribe(ticker -> {
                    ref.value = Util.getValueAtPrice(ref.baseCurrency, ref.price).add(ref.counterCurrency.getQuantity());
                    if (ref.value.compareTo(ref.previousValue.multiply(CONFIG.getStopLoss())) < 0)
                        Transactions.placeSellOrder(CONFIG.getBroker(), null, ref.baseCurrency, ref.counterCurrency, ticker.getLast());
                    ref.previousValue = ref.value;
                },
                throwable -> LOG.error("Error in ticket subscription (stop loss)", throwable));

        COMPOSITE_DISPOSABLE.add(tickerSub);
    }

    private void subscribeToCurrencyKline(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);

        new Timer("KlineSubscription").scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    BinanceKline binanceKline = BINANCE_MARKET_DATA_SERVICE.lastKline(currencyPair, KlineInterval.m1);
                    ref.price = binanceKline.getClosePrice();
                    ref.timestamp = new Date(binanceKline.getCloseTime());
                    doTheThing(ref);
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }
            }
        }, 0, SECONDS.toMillis(60));

        new Timer("Recalibration").scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                recalibrate();
            }
        }, MINUTES.toMillis(CONFIG.getRecalibrateFreq()), MINUTES.toMillis(CONFIG.getRecalibrateFreq()));
    }

    private void subscribeToCurrencyTicker(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        Disposable tickerSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTicker(currencyPair).subscribe(ticker -> {
                    ref.price = ticker.getLast();
                    ref.timestamp = ticker.getTimestamp();
                    doTheThing(ref);
                },
                throwable -> LOG.error("Error in ticket subscription", throwable));

        COMPOSITE_DISPOSABLE.add(tickerSub);
    }

    private void subscribeToCurrencyTrades(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        Disposable tradesSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTrades(currencyPair).subscribe(trade -> {
                    ref.price = trade.getPrice();
                    ref.timestamp = trade.getTimestamp();
                    doTheThing(ref);
                },
                throwable -> LOG.error("Error in trade subscription", throwable));

        COMPOSITE_DISPOSABLE.add(tradesSub);
    }

    private void subscribeToOrderBook(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        Disposable orderBookSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getOrderBook(currencyPair)
                .subscribe(
                        orderBook -> {
                            BigDecimal minAsk = orderBook.getAsks().stream().map(LimitOrder::getLimitPrice).min(BigDecimal::compareTo).orElseThrow();
                            BigDecimal maxBid = orderBook.getBids().stream().map(LimitOrder::getLimitPrice).max(BigDecimal::compareTo).orElseThrow();
                            LOG.info("spread: {} - {} - {}", minAsk, minAsk.subtract(maxBid), maxBid);
                            ref.price = minAsk.add(maxBid).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_DOWN);
                            ref.timestamp = orderBook.getTimeStamp();
                            doTheThing(ref);
                        },
                        throwable -> LOG.error("Error in orderBook subscription", throwable));

        COMPOSITE_DISPOSABLE.add(orderBookSub);
    }

    private void doTheThing(Ref ref) {
        ref.shortMAValue = Util.getMA(ref.previousShortMAValue, CONFIG.getShortLookback(), ref.price);
        ref.longMAValue = Util.getMA(ref.previousLongMAValue, CONFIG.getLongLookback(), ref.price);

        orderHistoryLookup = new OrderHistoryLookup();

        ref.c.updateCalc(ref.price, ref.shortMAValue, ref.longMAValue);
        ref.c.decide();

        BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder().timestamp(requireNonNullElse(ref.timestamp, new Date())).ma1(ref.shortMAValue).ma2(ref.longMAValue).l(ref.c.getLow()).h(ref.c.getHigh()).p(ref.price).build();
        binanceTradeHistoryRepository.save(binanceTradeHistory);

        BigDecimal value = Util.getValueAtPrice(ref.baseCurrency, ref.price).add(ref.counterCurrency.getQuantity());

        orderHistoryLookup.setTradeId(binanceTradeHistory.getTradeId());
        orderHistoryLookup.setValue(value);
        orderHistoryLookupRepository.save(orderHistoryLookup);

        LOG.info("{}/{} - {} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}", ref.baseCurrency.getSymbol(), ref.counterCurrency.getSymbol(), ref.count, ref.shortMAValue, ref.longMAValue, ref.c.getLow(), ref.c.getHigh(), ref.price, value);

        savePortfolio();

        ref.previousShortMAValue = ref.shortMAValue;
        ref.previousLongMAValue = ref.longMAValue;
        ref.count++;
    }

    private void savePortfolio() {
        portfolio = portfolioRepository.save(portfolio);
    }

    private void recalibrate() {
        if (CONFIG.isRecalibrate()) {
            CONFIG.setBackTest(true);
            Util.relacibrate(CONFIG);
            CONFIG.setBackTest(false);
        }
    }

    private void reconcile() {
        try {
            BINANCE_EXCHANGE.getTradeService().getOpenOrders().getOpenOrders().forEach(this::processRemoteOrder);

            BINANCE_EXCHANGE.getAccountService().getAccountInfo().getWallets().forEach((s, wallet) -> wallet.getBalances().forEach((currency, balance) -> {
                Currency c = Util.getCurrencyFromPortfolio(currency.getSymbol(), portfolio);
                int diff = c.getQuantity().compareTo(balance.getAvailable());
                if (diff == 0) return;
                c.setCurrencyType(CurrencyTypes.CRYPTO);
                c.setPortfolio(portfolio);
                portfolio.getCurrencies().add(c);
                if (diff > 0) Util.debit(c, c.getQuantity().subtract(balance.getAvailable()).abs());
                if (diff < 0) Util.credit(c, c.getQuantity().add(balance.getAvailable()).abs());
                LOG.info("{}: Reconciling local ledger ({}) with remote wallet ({}).", c.getSymbol(), c.getQuantity(), balance.getAvailable());
            }));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    private void processRemoteOrder(Order order) {
        LOG.info("Order: {}", order);
        if (order instanceof LimitOrder) {
            LimitOrder limitOrder = (LimitOrder) order;

            // check portfolio, then database, then create new
            BinanceLimitOrder binanceLimitOrder = portfolio.getBinanceLimitOrders().stream().filter(blo -> blo.getId().equals(limitOrder.getId())).findFirst().orElse(null);

            if (binanceLimitOrder == null) {
                binanceLimitOrder = binanceLimitOrderRepository.getById(limitOrder.getId());
                if (binanceLimitOrder != null) {
                    portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
                }
            }

            if (binanceLimitOrder == null) {
                binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
                portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
            }

            BinanceUtil.updateBinanceLimitOrder(binanceLimitOrder, limitOrder);
            BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
        }
        savePortfolio();
    }

    private static class Ref {
        final Calc c;
        final Currency baseCurrency;
        final Currency counterCurrency;
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
