package com.jessethouin.quant.backtest;

import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.BuyStrategyTypes;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.SellStrategyTypes;
import net.jacobpeterson.alpaca.enums.OrderSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.dto.Order;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

import static com.jessethouin.quant.backtest.BacktestParameterCombos.BACKTEST_RESULTS_QUEUE;
import static com.jessethouin.quant.backtest.BacktestParameterCombos.INTRADAY_PRICES;

public class ProcessHistoricIntradayPrices implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ProcessHistoricIntradayPrices.class);
    final BuyStrategyTypes buyStrategyType;
    final SellStrategyTypes sellStrategyType;
    final int shortLookback;
    final int longLookback;
    final BigDecimal highRisk;
    final BigDecimal lowRisk;
    final BigDecimal allowance;

    public ProcessHistoricIntradayPrices(BuyStrategyTypes buyStrategyType, SellStrategyTypes sellStrategyType, int shortLookback, int longLookback, BigDecimal highRisk, BigDecimal lowRisk, BigDecimal allowance) {
        this.buyStrategyType = buyStrategyType;
        this.sellStrategyType = sellStrategyType;
        this.shortLookback = shortLookback;
        this.longLookback = longLookback;
        this.highRisk = highRisk;
        this.lowRisk = lowRisk;
        this.allowance = allowance;
    }

    @Override
    public void run() {
        Config config = Config.getTheadSafeConfig();
        config.setBuyStrategy(buyStrategyType);
        config.setSellStrategy(sellStrategyType);
        config.setShortLookback(shortLookback);
        config.setLongLookback(longLookback);
        config.setLowRisk(lowRisk);
        config.setHighRisk(highRisk);
        config.setAllowance(allowance);

        Portfolio portfolio = Util.createPortfolio();

        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = INTRADAY_PRICES.get(0);
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;
        BigDecimal previousValue = BigDecimal.ZERO;

        Calc c;
        switch (config.getBroker()) {
            case ALPACA_TEST -> {
                Security aapl = Util.getSecurity(portfolio, "AAPL");
                c = new Calc(aapl, config, price);
            }
            case BINANCE_TEST -> {
                Currency base = Util.getCurrency(portfolio, "BTC");
                Currency counter = Util.getCurrency(portfolio, "USDT");
                c = new Calc(base, counter, config, BigDecimal.ZERO);
            }
            default -> throw new IllegalStateException("Unexpected value: " + config.getBroker());
        }

        for (int i = 0; i < INTRADAY_PRICES.size(); i++) {
            try {
                price = INTRADAY_PRICES.get(i);
                shortMAValue = Util.getMA(INTRADAY_PRICES, previousShortMAValue, i, shortLookback, price);
                longMAValue = Util.getMA(INTRADAY_PRICES, previousLongMAValue, i, longLookback, price);
                c.updateCalc(price, shortMAValue, longMAValue);

                switch (config.getBroker()) {
                    case ALPACA_TEST -> LOG.trace(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,000000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), Util.getPortfolioValue(portfolio, c.getSecurity().getCurrency(), price), shortMAValue, longMAValue, price, i));
                    case BINANCE_TEST -> LOG.trace("{} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}", i, shortMAValue, longMAValue, c.getLow(), c.getHigh(), price, Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity()));
                }

                c.decide();

                BigDecimal value = Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity());
                if (i == 0) previousValue = value;
                if (value.compareTo(previousValue.multiply(config.getStopLoss())) < 0) {
                    Transactions.placeCurrencySellOrder(config.getBroker(), c.getBase(), c.getCounter(), price);
                    LOG.error("Stop Loss");
                    System.exit(69); // NICE
                }

                previousShortMAValue = shortMAValue;
                previousLongMAValue = longMAValue;
                previousValue = value;
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }

        BigDecimal portfolioValue = BigDecimal.ZERO;
        BigDecimal bids = BigDecimal.ZERO;
        switch (config.getBroker()) {
            case ALPACA_TEST -> {
                portfolioValue = Util.getPortfolioValue(portfolio, c.getBase(), price);
                bids = BigDecimal.valueOf(portfolio.getAlpacaOrders().stream().filter(alpacaOrder -> alpacaOrder.getType().equals(OrderSide.BUY.getAPIName())).count());
            }
            case BINANCE_TEST -> {
                portfolioValue = Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity());
                bids = BigDecimal.valueOf(portfolio.getBinanceLimitOrders().stream().filter(binanceLimitOrder -> binanceLimitOrder.getType().equals(Order.OrderType.BID)).count());
            }
        }

        BacktestParameterResults backtestParameterResults = BacktestParameterResults.builder()
                .timestamp(new Date())
                .allowance(allowance)
                .gain(config.getGain())
                .loss(config.getLoss())
                .stopLoss(config.getStopLoss())
                .buyStrategyType(buyStrategyType)
                .sellStrategyType(sellStrategyType)
                .lowRisk(lowRisk)
                .highRisk(highRisk)
                .shortLookback(shortLookback)
                .longLookback(longLookback)
                .bids(bids)
                .value(portfolioValue)
                .build();

        BACKTEST_RESULTS_QUEUE.offer(backtestParameterResults);

        String msg = MessageFormat.format("{5}/{6} : {7} : {0,number,00} : {1,number,00} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", shortLookback, longLookback, lowRisk, highRisk, portfolioValue, buyStrategyType, sellStrategyType, allowance);
        LOG.trace(msg);
        BacktestParameterCombos.updateBest(msg, portfolioValue);
    }
}
