package com.jessethouin.quant.backtest;

import com.jessethouin.quant.alpaca.AlpacaCaptureHistory;
import com.jessethouin.quant.backtest.beans.repos.BacktestParameterResultsRepository;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.CurrencyType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.conf.Config.CONFIG;

@Component
public class BacktestStaticParameters extends AbstractBacktest {
    private static final Logger LOG = LogManager.getLogger(BacktestStaticParameters.class);

    public BacktestStaticParameters(TradeHistoryRepository tradeHistoryRepository, BacktestParameterResultsRepository backtestParameterResultsRepository, BinanceCaptureHistory binanceCaptureHistory, AlpacaCaptureHistory alpacaCaptureHistory) {
        super(tradeHistoryRepository, backtestParameterResultsRepository, binanceCaptureHistory, alpacaCaptureHistory);
    }

    public void runBacktest() {
        populateIntradayPrices();
        List<BigDecimal> intradayPrices = new ArrayList<>(INTRADAY_PRICES); // INTRADAY_PRICES will change if recalibrate is set to true, so we copy the values to a thread-safe local List

        long start = CONFIG.getBacktestStart().getTime();
        CONFIG.setBacktestStart(new Date(start - Duration.ofHours(CONFIG.getRecalibrateHours()).toMillis()));
        CONFIG.setBacktestEnd(new Date(start));

        BigDecimal shortMAValue = BigDecimal.ZERO;
        BigDecimal longMAValue = BigDecimal.ZERO;
        BigDecimal price = intradayPrices.getFirst();
        BigDecimal previousValue = BigDecimal.ZERO;

        Portfolio portfolio = Util.createPortfolio();
        Calc c;
        switch (CONFIG.getBroker()) {
            case ALPACA_SECURITY_TEST -> {
                Security aapl = Util.getSecurityFromPortfolio("AAPL", portfolio);
                c = new Calc(aapl, CONFIG, price);
            }
            case ALPACA_CRYPTO_TEST -> {
                Currency base = Util.getCurrencyFromPortfolio("USD", portfolio, CurrencyType.FIAT);
                Currency counter = Util.getCurrencyFromPortfolio("BTC", portfolio, CurrencyType.CRYPTO);
                c = new Calc(base, counter, CONFIG, BigDecimal.ZERO);
            }
            case BINANCE_TEST -> {
                Currency base = Util.getCurrencyFromPortfolio("BTC", portfolio, CurrencyType.CRYPTO);
                Currency counter = Util.getCurrencyFromPortfolio("USDT", portfolio, CurrencyType.CRYPTO);
                c = new Calc(base, counter, CONFIG, BigDecimal.ZERO);
            }
            default -> throw new IllegalStateException("Unexpected value: " + CONFIG.getBroker());
        }

        for (int i = 0; i < intradayPrices.size(); i++) {
            if (CONFIG.isRecalibrate() && i % CONFIG.getRecalibrateFreq() == 0) Util.recalibrate(CONFIG, false);

            price = intradayPrices.get(i);
            shortMAValue = Util.getMA(shortMAValue, CONFIG.getShortLookback(), price);
            longMAValue = Util.getMA(longMAValue, CONFIG.getLongLookback(), price);
            c.updateCalc(price, shortMAValue, longMAValue);

            switch (CONFIG.getBroker()) {
                case ALPACA_SECURITY_TEST -> LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,000000.000}", CONFIG.getShortLookback(), CONFIG.getLongLookback(), CONFIG.getLowRisk(), CONFIG.getHighRisk(), Util.getPortfolioValue(portfolio, c.getSecurity().getCurrency(), price), shortMAValue, longMAValue, price, i));
                case BINANCE_TEST, ALPACA_CRYPTO_TEST -> LOG.info("{} : ma1({}) {} : ma2({}) {} : l({}) {} : h({}) {} : p {} : v {} (base: {} counter: {})", i, CONFIG.getShortLookback(), shortMAValue, CONFIG.getLongLookback(), longMAValue, CONFIG.getLowRisk(), c.getLow(), CONFIG.getHighRisk(), c.getHigh(), price, Util.getValueAtPrice(c.getCounter(), price).add(c.getBase().getQuantity()), c.getBase().getQuantity().toPlainString(), c.getCounter().getQuantity().toPlainString());
            }

            c.decide();

            previousValue = stopLoss(price, previousValue, c);
        }

        super.logMarketChange(intradayPrices.getLast(), intradayPrices.getFirst(), LOG);

        switch (CONFIG.getBroker()) {
            case ALPACA_SECURITY_TEST -> LOG.info(c.getSecurity().getSecurityPosition().getPrice() + ", " + c.getSecurity().getSecurityPosition().getQuantity() + " : " + c.getSecurity().getSecurityPosition().getPrice().multiply(c.getSecurity().getSecurityPosition().getQuantity()));
            case ALPACA_CRYPTO_TEST -> {
                LOG.info("base   : value: {}", Util.formatFiat(c.getBase().getQuantity()));
                LOG.info("counter: value: {}", Util.formatFiat(c.getCounter().getQuantity()));
                LOG.info("orders : {}", portfolio.getAlpacaOrders().size());
            }
            case BINANCE_TEST -> {
                LOG.info("base   : value: {}", Util.formatFiat(c.getBase().getQuantity()));
                LOG.info("counter: value: {}", Util.formatFiat(c.getCounter().getQuantity()));
                LOG.info("orders : {}", portfolio.getBinanceLimitOrders().size());
                LOG.info("fees   : {}", Util.formatFiat(portfolio.getBinanceLimitOrders().stream().map(BinanceLimitOrder::getCommissionAmount).reduce(BigDecimal.ZERO, BigDecimal::add).multiply(price)));
            }
        }

        BigDecimal portfolioValue = BigDecimal.ZERO;
        switch (CONFIG.getBroker()) {
            case ALPACA_SECURITY_TEST -> portfolioValue = Util.getPortfolioValue(portfolio, c.getBase(), price);
            case BINANCE_TEST, ALPACA_CRYPTO_TEST -> portfolioValue = Util.getValueAtPrice(c.getCounter(), price).add(c.getBase().getQuantity());
        }
        LOG.info(MessageFormat.format("{0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4}", CONFIG.getShortLookback(), CONFIG.getLongLookback(), CONFIG.getLowRisk(), CONFIG.getHighRisk(), Util.formatFiat(portfolioValue)));

        System.exit(0);
    }

    private static BigDecimal stopLoss(BigDecimal price, BigDecimal previousValue, Calc c) {
        BigDecimal value = Util.getValueAtPrice(c.getCounter(), price).add(c.getBase().getQuantity());
        if (value.compareTo(previousValue.multiply(CONFIG.getStopLoss())) < 0) {
            LOG.warn("Something gross happened to the market or data. Invoking stop loss.");
            Transactions.placeSellOrder(CONFIG.getBroker(), null, c.getBase(), c.getCounter(), price);
            System.exit(69); // NICE
        }
        return value;
    }
}
