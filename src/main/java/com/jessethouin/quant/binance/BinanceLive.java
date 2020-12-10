package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyTypes;
import com.jessethouin.quant.db.Database;
import info.bitrich.xchangestream.binance.BinanceStreamingExchange;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.conf.Broker.BINANCE_TEST;

public class BinanceLive {
    private static final Logger LOG = LogManager.getLogger(BinanceLive.class);
    public static final BinanceLive INSTANCE = new BinanceLive();
    private static final Config config = new Config();
    private static Portfolio portfolio;

    private static final BinanceExchange BINANCE_EXCHANGE;
    private static final BinanceStreamingExchange BINANCE_STREAMING_EXCHANGE;

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

    //delete from SECURITY_POSITION; delete from SECURITY; delete from CURRENCY_POSITION; delete from CURRENCY; delete from PORTFOLIO;
    public static void doLive() {
        config.setBroker(BINANCE_TEST);
        portfolio = Database.getPortfolio();

        if (portfolio == null) {
            portfolio = new Portfolio();

            List<String> fiatCurrencies = config.getFiatCurrencies();
            fiatCurrencies.forEach(c -> {
                Currency currency = new Currency();
                currency.setSymbol(c);
                currency.setCurrencyType(CurrencyTypes.FIAT);
                if (c.equals("USD")) { // default-coded for now, until international exchanges are implemented
                    List<String> tickers = config.getSecurities();
                    tickers.forEach(t -> {
                        Security security = new Security();
                        security.setSymbol(t);
                        security.setCurrency(currency);
                        security.setPortfolio(portfolio);
                        portfolio.getSecurities().add(security);
                    });
                }
                currency.setPortfolio(portfolio);
                portfolio.getCurrencies().add(currency);
            });

            List<String> cryptoCurrencies = config.getCryptoCurrencies();
            cryptoCurrencies.forEach(c -> {
                Currency currency = new Currency();
                currency.setSymbol(c);
                currency.setCurrencyType(CurrencyTypes.CRYPTO);
/*
                try {
                    BINANCE_EXCHANGE.getAccountService().getAccountInfo().getWallet().getBalances().forEach((curr, bal) -> {
                        if (curr.getSymbol().equals(c)) Transactions.addCurrency(portfolio, bal.getAvailable(), currency, null, BigDecimal.ZERO);
                    });
                } catch (IOException e) {
                    LOG.error(e.getLocalizedMessage());
                }
*/
                currency.setPortfolio(portfolio);
                portfolio.getCurrencies().add(currency);
            });

            try {
                Currency usd = Util.getCurrencyFromPortfolio("USD", portfolio);
                Currency btc = Util.getCurrencyFromPortfolio("BTC", portfolio);
                Currency usdt = Util.getCurrencyFromPortfolio("USDT", portfolio);
                Ticker ticker = BINANCE_EXCHANGE.getMarketDataService().getTicker(CurrencyPair.BTC_USDT);
                Transactions.addCurrencyPosition(portfolio, BigDecimal.TEN, btc, usd, ticker.getLast().subtract(BigDecimal.TEN));
            } catch (IOException e) {
                LOG.error(e.getLocalizedMessage());
            }

            Database.persistPortfolio(portfolio);
        }

        List<CurrencyPair> currencyPairs = Util.getAllCurrencyPairs(config);

        ProductSubscription.ProductSubscriptionBuilder productSubscriptionBuilder = ProductSubscription.create();
        currencyPairs.forEach(productSubscriptionBuilder::addAll);

        BINANCE_STREAMING_EXCHANGE.connect(productSubscriptionBuilder.build()).blockingAwait();
        currencyPairs.forEach(currencyPair -> {
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
                                LOG.info("spread: {} - {} - {}", askPrices.get(0), askPrices.get(0).subtract(bidPrices.get(0)), bidPrices.get(0));
                            },
                            throwable -> LOG.error("Error in orderBook subscription", throwable));


            Disposable orderSub = BINANCE_STREAMING_EXCHANGE.getStreamingTradeService()
                    .getOrderChanges(currencyPair)
                    .subscribe(order -> {
                        LOG.info("Order: {}", order);
                        // order handling here
                    });
        });


        currencyPairs.forEach(BinanceLive::subscribeToCurrencyTrades);
    }

    private static void subscribeToCurrencyTrades(CurrencyPair currencyPair) {
        Currency baseCurrency = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        Currency counterCurrency = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);

        var ref = new Object() {
            final Calc c = new Calc(baseCurrency, counterCurrency, config, BigDecimal.ZERO);
            final List<BigDecimal> intradayPrices = new ArrayList<>();
            int count = 0;
            BigDecimal shortMAValue;
            BigDecimal longMAValue;
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal previousShortMAValue = BigDecimal.ZERO;
            BigDecimal previousLongMAValue = BigDecimal.ZERO;
        };

        Disposable tradeSub = BINANCE_STREAMING_EXCHANGE.getStreamingMarketDataService()
                .getTrades(currencyPair)
                .subscribe(trade -> {
                    ref.price = trade.getPrice();
                    if (ref.count < config.getLongLookback()) ref.intradayPrices.add(ref.price);

                    ref.shortMAValue = Util.getMA(ref.intradayPrices, ref.previousShortMAValue, ref.count, config.getShortLookback(), ref.price);
                    ref.longMAValue = Util.getMA(ref.intradayPrices, ref.previousLongMAValue, ref.count, config.getLongLookback(), ref.price);
                    ref.c.updateCalc(ref.price, ref.shortMAValue, ref.longMAValue, portfolio);
//                    ref.c.decide();
                    LOG.info(MessageFormat.format("{0,number,000} : ma1 {1,number,000.0000} : ma2 {2,number,000.0000} : l {3,number,000.0000}: h {4,number,000.0000}: p {5,number,000.0000} : {6,number,00000.0000}", ref.count, ref.shortMAValue, ref.longMAValue, ref.c.getLow(), ref.c.getHigh(), ref.price, Util.getPortfolioValue(portfolio, baseCurrency).add(Util.getPortfolioValue(portfolio, counterCurrency))));

                    BinanceTradeHistory binanceTradeHistory = new BinanceTradeHistory.Builder().setTimestamp(new Date()).setMa1(ref.shortMAValue).setMa2(ref.longMAValue).setL(ref.c.getLow()).setH(ref.c.getHigh()).setP(ref.price).build();
                    Database.persistTradeHistory(binanceTradeHistory);

//                    Database.persistPortfolio(portfolio);
                    ref.previousShortMAValue = ref.shortMAValue;
                    ref.previousLongMAValue = ref.longMAValue;
                    ref.count++;
                });
    }
}
