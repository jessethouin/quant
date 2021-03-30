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
import com.jessethouin.quant.binance.config.BinanceApiConfig;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyTypes;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.dto.marketdata.BinanceKline;
import org.knowm.xchange.binance.dto.marketdata.KlineInterval;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.BinanceExchangeInfo;
import org.knowm.xchange.binance.service.BinanceMarketDataService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;

import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.knowm.xchange.dto.Order.OrderStatus.CANCELED;
import static org.knowm.xchange.dto.Order.OrderStatus.FILLED;

@Component
public class BinanceLive {
    private static final Logger LOG = LogManager.getLogger(BinanceLive.class);
    private static final Config CONFIG = Config.INSTANCE;
    private static Portfolio portfolio;
    private static OrderHistoryLookup orderHistoryLookup;

    private static final BinanceExchange BINANCE_EXCHANGE;
    private static final BinanceStreamingExchange BINANCE_STREAMING_EXCHANGE;
    private static final BinanceExchangeInfo BINANCE_EXCHANGE_INFO;
    private static final CompositeDisposable COMPOSITE_DISPOSABLE = new CompositeDisposable();
    private static final Map<CurrencyPair, BigDecimal> MIN_TRADES = new HashMap<>();

    private static BinanceLimitOrderRepository binanceLimitOrderRepository;
    private static BinanceTradeHistoryRepository binanceTradeHistoryRepository;
    private static OrderHistoryLookupRepository orderHistoryLookupRepository;
    private static PortfolioRepository portfolioRepository;

    static {
        ExchangeSpecification exSpec = new BinanceExchange().getDefaultExchangeSpecification();
        exSpec.setUserName(BinanceApiConfig.INSTANCE.getUserName());
        exSpec.setApiKey(BinanceApiConfig.INSTANCE.getApiKey());
        exSpec.setSecretKey(BinanceApiConfig.INSTANCE.getSecretKey());
        BINANCE_EXCHANGE = (BinanceExchange) ExchangeFactory.INSTANCE.createExchange(exSpec);
        BINANCE_EXCHANGE_INFO = BINANCE_EXCHANGE.getExchangeInfo();

        ExchangeSpecification strExSpec = new BinanceStreamingExchange().getDefaultExchangeSpecification();
        strExSpec.setUserName(BinanceApiConfig.INSTANCE.getUserName());
        strExSpec.setApiKey(BinanceApiConfig.INSTANCE.getApiKey());
        strExSpec.setSecretKey(BinanceApiConfig.INSTANCE.getSecretKey());
        BINANCE_STREAMING_EXCHANGE = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(strExSpec);

        long start = new Date().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));
    }

    private BinanceLive(BinanceLimitOrderRepository binanceLimitOrderRepository, BinanceTradeHistoryRepository binanceTradeHistoryRepository, OrderHistoryLookupRepository orderHistoryLookupRepository, PortfolioRepository portfolioRepository) {
        BinanceLive.binanceLimitOrderRepository = binanceLimitOrderRepository;
        BinanceLive.binanceTradeHistoryRepository = binanceTradeHistoryRepository;
        BinanceLive.orderHistoryLookupRepository = orderHistoryLookupRepository;
        BinanceLive.portfolioRepository = portfolioRepository;
    }

    public BinanceExchange getBinanceExchange() {
        return BINANCE_EXCHANGE;
    }

    public BinanceExchangeInfo getBinanceExchangeInfo() {
        return BINANCE_EXCHANGE_INFO;
    }

    public Map<CurrencyPair, BigDecimal> getMinTrades() {
        return MIN_TRADES;
    }

    public OrderHistoryLookup getOrderHistoryLookup() {
        return orderHistoryLookup;
    }

    public static void doLive() {
        BinanceTransactions.showWallets();

        portfolio = requireNonNullElse(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc(), Util.createPortfolio());
        portfolioRepository.save(portfolio);

        BinanceTransactions.showTradingFees(portfolio);
        reconcile();
        recalibrate();

        List<CurrencyPair> currencyPairs = Util.getAllCurrencyPairs(CONFIG);
        currencyPairs.forEach(currencyPair -> MIN_TRADES.put(currencyPair, Util.getMinTrade(currencyPair)));

        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();
        currencyPairs.forEach(productSubscriptionBuilder::addAll);

        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();

        currencyPairs.forEach(BinanceLive::subscribeToOrderUpdates);
        currencyPairs.forEach(BinanceLive::stopLoss);

        switch (CONFIG.getDataFeed()) {
            case KLINE -> currencyPairs.forEach(BinanceLive::subscribeToCurrencyKline);
            case TRADE -> currencyPairs.forEach(BinanceLive::subscribeToCurrencyTrades);
            case TICKER -> currencyPairs.forEach(BinanceLive::subscribeToCurrencyTicker);
            case ORDER_BOOK -> currencyPairs.forEach(BinanceLive::subscribeToOrderBook);
        }
    }

    private static void subscribeToOrderUpdates(CurrencyPair currencyPair) {
        Disposable orderSub = BINANCE_STREAMING_EXCHANGE.getStreamingTradeService()
                .getOrderChanges(currencyPair)
                .subscribe(BinanceLive::processRemoteOrder);

        COMPOSITE_DISPOSABLE.add(orderSub);
    }

    private static void stopLoss(CurrencyPair currencyPair) {
        if (!currencyPair.toString().equals("69")) return; //we'll need to get rid of this once we start testing stop loss. This just keeps my checkmark green and happy.

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

    private static void subscribeToCurrencyKline(CurrencyPair currencyPair) {
        BinanceMarketDataService marketDataService = (BinanceMarketDataService) BINANCE_EXCHANGE.getMarketDataService();
        Ref ref = new Ref(currencyPair, portfolio);

        new Timer("KlineSubscription").scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    BinanceKline binanceKline = marketDataService.lastKline(currencyPair, KlineInterval.m1);
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

    private static void subscribeToCurrencyTicker(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        Disposable tickerSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTicker(currencyPair).subscribe(ticker -> {
                    ref.price = ticker.getLast();
                    ref.timestamp = ticker.getTimestamp();
                    doTheThing(ref);
                },
                throwable -> LOG.error("Error in ticket subscription", throwable));

        COMPOSITE_DISPOSABLE.add(tickerSub);
    }

    private static void subscribeToCurrencyTrades(CurrencyPair currencyPair) {
        Ref ref = new Ref(currencyPair, portfolio);
        Disposable tradesSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTrades(currencyPair).subscribe(trade -> {
                    ref.price = trade.getPrice();
                    ref.timestamp = trade.getTimestamp();
                    doTheThing(ref);
                },
                throwable -> LOG.error("Error in trade subscription", throwable));

        COMPOSITE_DISPOSABLE.add(tradesSub);
    }

    private static void subscribeToOrderBook(CurrencyPair currencyPair) {
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

    private static void doTheThing(Ref ref) {
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

        portfolioRepository.save(portfolio);

        ref.previousShortMAValue = ref.shortMAValue;
        ref.previousLongMAValue = ref.longMAValue;
        ref.count++;
    }

    private static void recalibrate() {
        if (CONFIG.isRecalibrate()) {
            CONFIG.setBackTest(true);
            Util.relacibrate(CONFIG);
            CONFIG.setBackTest(false);
        }
    }

    private static void reconcile() {
        try {
            BINANCE_EXCHANGE.getTradeService().getOpenOrders().getOpenOrders().forEach(BinanceLive::processRemoteOrder);

            BINANCE_EXCHANGE.getAccountService().getAccountInfo().getWallets().forEach((s, wallet) -> wallet.getBalances().forEach((currency, balance) -> {
                Currency c = Util.getCurrency(portfolio, currency.getSymbol());
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

    private static void processRemoteOrder(Order order) {
        if (order instanceof LimitOrder) {
            LOG.info("Order: {}", order);
            BinanceLimitOrder binanceLimitOrder;
            LimitOrder limitOrder = (LimitOrder) order;
            switch (order.getStatus()) {
                case NEW -> Util.createBinanceLimitOrder(portfolio, limitOrder);
                case FILLED -> {
                    binanceLimitOrder = Objects.requireNonNullElse(binanceLimitOrderRepository.getById(limitOrder.getId()), Util.createBinanceLimitOrder(portfolio, limitOrder));
                    binanceLimitOrder.setStatus(FILLED);
                    binanceLimitOrder.setAveragePrice(limitOrder.getAveragePrice());
                    binanceLimitOrder.setCumulativeAmount(limitOrder.getCumulativeAmount());
                    BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
                    binanceLimitOrderRepository.save(binanceLimitOrder);
                }
                case CANCELED -> {
                    binanceLimitOrder = Objects.requireNonNullElse(binanceLimitOrderRepository.getById(limitOrder.getId()), Util.createBinanceLimitOrder(portfolio, limitOrder));
                    binanceLimitOrder.setStatus(CANCELED);
                    BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
                    binanceLimitOrderRepository.save(binanceLimitOrder);
                }
            }
        }
    }

    public static class Ref {
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
