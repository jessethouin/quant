package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.CurrencyPosition;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.OrderHistoryLookup;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.calculators.SMA;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.Observable;
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
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.marketdata.Trade;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static java.util.Objects.requireNonNullElse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.knowm.xchange.dto.Order.OrderStatus.CANCELED;
import static org.knowm.xchange.dto.Order.OrderStatus.FILLED;

public class BinanceLive {
    private static final Logger LOG = LogManager.getLogger(BinanceLive.class);
    public static final BinanceLive INSTANCE = new BinanceLive();
    private static final Config config = Config.INSTANCE;
    private static Portfolio portfolio;
    private static OrderHistoryLookup orderHistoryLookup;
    private static BinanceExchangeInfo binanceExchangeInfo;

    private static final BinanceExchange BINANCE_EXCHANGE;
    private static final BinanceStreamingExchange BINANCE_STREAMING_EXCHANGE;
    private static final String TICKER = "TICKER";
    private static final String TRADES = "TRADES";

    static {
        ExchangeSpecification exSpec = new BinanceExchange().getDefaultExchangeSpecification();
        exSpec.setUserName("54704697");
        exSpec.setApiKey("M4gIEsmhsp5MjIkSZRapUUxScnZno56OHwJOvh1Bp3qIxW54FGCZnOxUYneNjVXB");
        exSpec.setSecretKey("tlAe5qFbA8oVDH0M085pYANzRD0EPHVteicsKk6rlKg1gEdC3j1lkSF3FMpd7jkO");
        BINANCE_EXCHANGE = (BinanceExchange) ExchangeFactory.INSTANCE.createExchange(exSpec);

        ExchangeSpecification strExSpec = new BinanceStreamingExchange().getDefaultExchangeSpecification();
        strExSpec.setUserName("54704697");
        strExSpec.setApiKey("M4gIEsmhsp5MjIkSZRapUUxScnZno56OHwJOvh1Bp3qIxW54FGCZnOxUYneNjVXB");
        strExSpec.setSecretKey("tlAe5qFbA8oVDH0M085pYANzRD0EPHVteicsKk6rlKg1gEdC3j1lkSF3FMpd7jkO");
        BINANCE_STREAMING_EXCHANGE = (BinanceStreamingExchange) StreamingExchangeFactory.INSTANCE.createExchange(strExSpec);
    }

    private BinanceLive() {
    }

    public BinanceExchange getBinanceExchange() {
        return BINANCE_EXCHANGE;
    }

    public BinanceStreamingExchange getBinanceStreamingExchange() {
        return BINANCE_STREAMING_EXCHANGE;
    }

    public OrderHistoryLookup getOrderHistoryLookup() {
        return orderHistoryLookup;
    }

    public BinanceExchangeInfo getBinanceExchangeInfo() {
        return binanceExchangeInfo;
    }

    public static void doLive() {
        binanceExchangeInfo = BinanceLive.INSTANCE.getBinanceExchange().getExchangeInfo();
        portfolio = requireNonNullElse(Database.getPortfolio(), Util.createPortfolio());
        Database.persistPortfolio(portfolio);

        List<CurrencyPair> currencyPairs = Util.getAllCurrencyPairs(config);

        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();
        currencyPairs.forEach(productSubscriptionBuilder::addAll);

        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();

        currencyPairs.forEach(BinanceLive::subscribeToOrderBook);
        currencyPairs.forEach(BinanceLive::subscribeToOrderUpdates);
//        currencyPairs.forEach(BinanceLive::subscribeToCurrencyTicker);
//        currencyPairs.forEach(BinanceLive::subscribeToCurrencyTrades);
        currencyPairs.forEach(BinanceLive::subscribeToCurrencyKline);
    }

    private static void subscribeToOrderUpdates(CurrencyPair currencyPair) {
        Disposable orderSub = BINANCE_STREAMING_EXCHANGE.getStreamingTradeService()
                .getOrderChanges(currencyPair)
                .subscribe(order -> {
                    if (order instanceof LimitOrder) {
                        LOG.info("Order: {}", order);
                        BinanceLimitOrder binanceLimitOrder;
                        LimitOrder limitOrder = (LimitOrder) order;
                        switch (order.getStatus()) {
                            case NEW -> {
                                List<CurrencyPosition> sellableCurrencyPositions = Transactions.getSellableCurrencyPositions(currencyPair, portfolio, limitOrder.getLimitPrice(), false);
                                createBinanceLimitOrder(limitOrder, sellableCurrencyPositions);
                            }
                            case FILLED -> {
                                binanceLimitOrder = Objects.requireNonNullElse(Database.getBinanceLimitOrder(limitOrder.getId()), createBinanceLimitOrder(limitOrder, Collections.emptyList()));
                                binanceLimitOrder.setStatus(FILLED);
                                binanceLimitOrder.setAveragePrice(limitOrder.getAveragePrice());
                                binanceLimitOrder.setCumulativeAmount(limitOrder.getCumulativeAmount());
                                BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
                                Database.persistBinanceLimitOrder(binanceLimitOrder);
                            }
                            case CANCELED -> {
                                binanceLimitOrder = Objects.requireNonNullElse(Database.getBinanceLimitOrder(limitOrder.getId()), createBinanceLimitOrder(limitOrder, Collections.emptyList()));
                                binanceLimitOrder.setStatus(CANCELED);
                                BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
                                Database.persistBinanceLimitOrder(binanceLimitOrder);
                            }
                        }
                    }
                });
    }

    private static BinanceLimitOrder createBinanceLimitOrder(LimitOrder limitOrder, List<CurrencyPosition> sellableCurrencyPositions) {
        LOG.info("Creating new BinanceLimitOrder for order {} status {}", limitOrder.getId(), limitOrder.getStatus());
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder, sellableCurrencyPositions);
        Database.persistBinanceLimitOrder(binanceLimitOrder);
        return binanceLimitOrder;
    }

    private static void subscribeToCurrencyKline(CurrencyPair currencyPair) {
        BinanceMarketDataService marketDataService = (BinanceMarketDataService) BINANCE_EXCHANGE.getMarketDataService();
        Ref ref = new Ref(Util.getCurrency(portfolio, currencyPair.base.getSymbol()), Util.getCurrency(portfolio, currencyPair.counter.getSymbol()));
        try {
            marketDataService.klines(currencyPair, KlineInterval.m1, 99, null, null).stream().map(BinanceKline::getClosePrice).forEach(ref.intradayPrices::add);
            ref.count = ref.intradayPrices.size();
            ref.previousShortMAValue = SMA.sma(ref.intradayPrices.subList(ref.count - config.getShortLookback(), ref.count), config.getShortLookback());
            ref.previousLongMAValue = SMA.sma(ref.intradayPrices.subList(ref.count - config.getLongLookback(), ref.count), config.getLongLookback());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        new Timer().scheduleAtFixedRate(new TimerTask() {
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
    }

    private static void subscribeToCurrencyTicker(CurrencyPair currencyPair) {
        subscribeToMarketFeed(currencyPair, TICKER);
    }

    private static void subscribeToCurrencyTrades(CurrencyPair currencyPair) {
        subscribeToMarketFeed(currencyPair, TRADES);
    }

    private static void subscribeToMarketFeed(CurrencyPair currencyPair, String subscriptionType) {
        LOG.info("Subscribing to {} for currencyPair {}", subscriptionType, currencyPair);
        Currency baseCurrency = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        Currency counterCurrency = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
        Observable<?> subscription;

        switch (subscriptionType) {
            case TICKER -> subscription = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTicker(currencyPair);
            case TRADES -> subscription = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService().getTrades(currencyPair);
            default -> throw new IllegalArgumentException("Subscription type must be one of TICKER, TRADES");
        }

        Ref ref = new Ref(baseCurrency, counterCurrency);

        Disposable subscribe = subscription.subscribe(t -> {
            switch (subscriptionType) {
                case "TICKER" -> {
                    ref.price = ((Ticker) t).getLast();
                    ref.timestamp = ((Ticker) t).getTimestamp();
                }
                case "TRADES" -> {
                    ref.price = ((Trade) t).getPrice();
                    ref.timestamp = ((Trade) t).getTimestamp();
                }
                default -> throw new IllegalArgumentException("Subscription type must be one of: Trades, Ticker");
            }

            doTheThing(ref);
        });
    }

    private static void subscribeToOrderBook(CurrencyPair currencyPair) {
        Disposable orderBookSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getOrderBook(currencyPair)
                .subscribe(
                        orderBook -> {
                            List<LimitOrder> asks = orderBook.getAsks();
                            asks.sort(Comparator.comparing(LimitOrder::getLimitPrice));
                            List<BigDecimal> askPrices = new ArrayList<>();
                            asks.forEach(l -> askPrices.add(l.getLimitPrice()));

                            List<LimitOrder> bids = orderBook.getBids();
                            bids.sort(Comparator.comparing(LimitOrder::getLimitPrice).reversed());
                            List<BigDecimal> bidPrices = new ArrayList<>();
                            bids.forEach(l -> bidPrices.add(l.getLimitPrice()));
                            LOG.trace("spread: {} - {} - {}", askPrices.get(0), askPrices.get(0).subtract(bidPrices.get(0)), bidPrices.get(0));
                        },
                        throwable -> LOG.error("Error in orderBook subscription", throwable));
    }

    private static class Ref {
        final Calc c;
        final List<BigDecimal> intradayPrices = new ArrayList<>();
        Currency baseCurrency;
        Currency counterCurrency;
        int count = 0;
        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        Date timestamp;

        public Ref(Currency baseCurrency, Currency counterCurrency) {
            this.baseCurrency = baseCurrency;
            this.counterCurrency = counterCurrency;
            c = new Calc(baseCurrency, counterCurrency, config, BigDecimal.ZERO);
        }
    }

    private static void doTheThing(Ref ref) {
        if (ref.count < config.getLongLookback()) ref.intradayPrices.add(ref.price);

        ref.shortMAValue = Util.getMA(ref.intradayPrices, ref.previousShortMAValue, ref.count, config.getShortLookback(), ref.price);
        ref.longMAValue = Util.getMA(ref.intradayPrices, ref.previousLongMAValue, ref.count, config.getLongLookback(), ref.price);

        BinanceTradeHistory binanceTradeHistory = new BinanceTradeHistory.Builder().setTimestamp(requireNonNullElse(ref.timestamp, new Date())).setMa1(ref.shortMAValue).setMa2(ref.longMAValue).setL(ref.c.getLow()).setH(ref.c.getHigh()).setP(ref.price).build();
        Database.persistTradeHistory(binanceTradeHistory);

        BigDecimal value = Util.getBalance(portfolio, ref.baseCurrency, ref.counterCurrency, ref.price).add(Util.getBalance(portfolio, ref.counterCurrency));
        orderHistoryLookup = new OrderHistoryLookup(binanceTradeHistory.getTradeId(), 0, value);

        LOG.info("{}/{} - {} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}", ref.baseCurrency.getSymbol(), ref.counterCurrency.getSymbol(), ref.count, ref.shortMAValue, ref.longMAValue, ref.c.getLow(), ref.c.getHigh(), ref.price, value);

        ref.c.updateCalc(ref.price, ref.shortMAValue, ref.longMAValue, portfolio);
        ref.c.decide();
        Database.persistPortfolio(portfolio);
        Database.persistOrderHistoryLookup(orderHistoryLookup);

        ref.previousShortMAValue = ref.shortMAValue;
        ref.previousLongMAValue = ref.longMAValue;
        ref.count++;
    }
}
