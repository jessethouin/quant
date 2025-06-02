package com.jessethouin.quant.backtest;

import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.BuyStrategyType;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyType;
import com.jessethouin.quant.conf.SellStrategyType;
import net.jacobpeterson.alpaca.openapi.trader.model.OrderSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.dto.Order;

import java.math.BigDecimal;
import java.util.Date;

import static com.jessethouin.quant.backtest.BacktestParameterCombos.BACKTEST_RESULTS_QUEUE;
import static com.jessethouin.quant.backtest.BacktestParameterCombos.INTRADAY_PRICES;
import static com.jessethouin.quant.conf.Config.CONFIG;

public class ProcessHistoricIntradayPrices implements Runnable {
    private static final Logger LOG = LogManager.getLogger(ProcessHistoricIntradayPrices.class);
    final BuyStrategyType buyStrategyType;
    final SellStrategyType sellStrategyType;
    final int shortLookback;
    final int longLookback;
    final BigDecimal highRisk;
    final BigDecimal lowRisk;
    final BigDecimal allowance;

    public ProcessHistoricIntradayPrices(BuyStrategyType buyStrategyType, SellStrategyType sellStrategyType, int shortLookback, int longLookback, BigDecimal highRisk, BigDecimal lowRisk, BigDecimal allowance) {
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
        BigDecimal price = INTRADAY_PRICES.getFirst();
        BigDecimal shortMAValue = BigDecimal.ZERO;
        BigDecimal longMAValue = BigDecimal.ZERO;

        Calc c;
        switch (config.getBroker()) {
            case ALPACA_SECURITY_TEST -> {
                Security aapl = Util.getSecurityFromPortfolio("AAPL", portfolio);
                c = new Calc(aapl, config, price);
            }
            case ALPACA_CRYPTO_TEST -> {
                Currency base = Util.getCurrencyFromPortfolio("USD", portfolio, CurrencyType.FIAT);
                Currency counter = Util.getCurrencyFromPortfolio("BTC", portfolio, CurrencyType.CRYPTO);
                c = new Calc(base, counter, CONFIG, BigDecimal.ZERO);
            }
            case BINANCE_TEST -> {
                Currency base = Util.getCurrencyFromPortfolio("BTC", portfolio, CurrencyType.CRYPTO);
                Currency counter = Util.getCurrencyFromPortfolio("USDT", portfolio, CurrencyType.CRYPTO);
                c = new Calc(base, counter, config, BigDecimal.ZERO);
            }
            default -> throw new IllegalStateException("Unexpected value: " + config.getBroker());
        }

        for (BigDecimal intradayPrice : INTRADAY_PRICES) {
            try {
                price = intradayPrice;
                shortMAValue = Util.getMA(shortMAValue, shortLookback, price);
                longMAValue = Util.getMA(longMAValue, longLookback, price);
                c.updateCalc(price, shortMAValue, longMAValue);
                c.decide();
            } catch (Exception e) {
                LOG.error("Error while looping INTRADAY_PRICES {}", e.getMessage());
            }
        }

        BigDecimal portfolioValue;
        BigDecimal bids;
        BigDecimal fees;
        switch (config.getBroker()) {
            case ALPACA_SECURITY_TEST -> {
                portfolioValue = Util.getPortfolioValue(portfolio, c.getBase(), price);
                bids = BigDecimal.valueOf(portfolio.getAlpacaOrders().stream().filter(alpacaOrder -> alpacaOrder.getSide().equals(OrderSide.BUY)).count());
                fees = BigDecimal.ZERO; // todo: aLpAcA hAs No FeEs. :(
            }
            case ALPACA_CRYPTO_TEST -> {
                portfolioValue = Util.getValueAtPrice(c.getCounter(), price).add(c.getBase().getQuantity());
                bids = BigDecimal.valueOf(portfolio.getAlpacaOrders().stream().filter(alpacaOrder -> alpacaOrder.getSide().equals(OrderSide.BUY)).count());
                fees = BigDecimal.ZERO; // todo: aLpAcA hAs No FeEs. :(
            }
            case BINANCE_TEST -> {
                portfolioValue = Util.getValueAtPrice(c.getBase(), price).add(c.getCounter().getQuantity());
                bids = BigDecimal.valueOf(portfolio.getBinanceLimitOrders().stream().filter(binanceLimitOrder -> binanceLimitOrder.getType().equals(Order.OrderType.BID)).count());
                fees = portfolio.getBinanceLimitOrders().stream().map(binanceLimitOrder -> binanceLimitOrder.getCommissionAmount().multiply(binanceLimitOrder.getLimitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
            }
            default -> {
                portfolioValue = BigDecimal.ZERO;
                bids = BigDecimal.ZERO;
                fees = BigDecimal.ZERO;
            }
        }

        BacktestParameterResults backtestParameterResults = BacktestParameterResults.builder()
                .timestamp(new Date())
                .allowance(allowance)
                .stopLoss(config.getStopLoss())
                .buyStrategyType(buyStrategyType)
                .sellStrategyType(sellStrategyType)
                .lowRisk(lowRisk)
                .highRisk(highRisk)
                .shortLookback(shortLookback)
                .longLookback(longLookback)
                .bids(bids)
                .fees(fees)
                .value(portfolioValue)
                .build();

        BACKTEST_RESULTS_QUEUE.offer(backtestParameterResults);
    }
}
