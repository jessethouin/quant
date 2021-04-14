package com.jessethouin.quant.binance;

import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_MIN_TRADES;
import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_TRADE_SERVICE;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Util;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;

@Component
public class BinanceTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTransactions.class);

    public static void buyCurrency(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(currencyPair, qty, price, BID);
    }

    public static void sellCurrency(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(currencyPair, qty, price, ASK);
    }

    private static void transact(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (qty.multiply(price).compareTo(BINANCE_MIN_TRADES.get(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", BINANCE_MIN_TRADES.get(currencyPair), currencyPair.toString(), qty.multiply(price));
            return;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty.setScale(6, RoundingMode.FLOOR))
                .limitPrice(price.setScale(8, RoundingMode.FLOOR))
                .timestamp(new Date())
                .build();
        try {
            LOG.info("Limit Order: " + limitOrder.toString());
            String id = BINANCE_TRADE_SERVICE.placeLimitOrder(limitOrder);
            if (id == null) throw new Exception("Limit Order id was null from server.");
            BinanceStreamProcessing.getOrderHistoryLookup().setOrderId(Long.parseLong(id));
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void buyTestCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        testTransact(portfolio, currencyPair, qty, price, BID);
    }

    public static void sellTestCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        testTransact(portfolio, currencyPair, qty, price, ASK);
    }

    private static void testTransact(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (!CONFIG.isBackTest() && qty.multiply(price).compareTo(BINANCE_MIN_TRADES.get(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", BINANCE_MIN_TRADES.get(currencyPair), currencyPair.toString(), qty.multiply(price));
            return;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .id(String.valueOf(Math.random() * Integer.MIN_VALUE))
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty)
                .limitPrice(price)
                .fee(price.multiply(qty).multiply(CONFIG.getFee()).setScale(8, RoundingMode.HALF_UP))
                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        LOG.trace("Test Limit Order: " + limitOrder.toString());
        processTestTransaction(limitOrder, portfolio);
    }

    private static void processTestTransaction(LimitOrder limitOrder, Portfolio portfolio) {
        // Mocking remote NEW order
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
        LOG.trace("New Test Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        if (!CONFIG.isBackTest()) {
            BinanceStreamProcessing.getOrderHistoryLookup().setOrderId(Long.parseLong(binanceLimitOrder.getId()));
        }
        processBinanceLimitOrder(binanceLimitOrder);

        // Mocking remote FILLED order
        binanceLimitOrder.setStatus(Order.OrderStatus.FILLED);
        binanceLimitOrder.setAveragePrice(BigDecimal.ONE);
        binanceLimitOrder.setCumulativeAmount(limitOrder.getOriginalAmount());
        processBinanceLimitOrder(binanceLimitOrder);
    }

    public static void processBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder) {
        Portfolio portfolio = binanceLimitOrder.getPortfolio();
        CurrencyPair currencyPair = new CurrencyPair(binanceLimitOrder.getInstrument());
        Currency base = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        Currency counter = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
        Currency commissionAsset = Objects.requireNonNullElse(binanceLimitOrder.getCommissionAsset(), counter);
        BigDecimal limitPrice = binanceLimitOrder.getLimitPrice();
        BigDecimal originalAmount = binanceLimitOrder.getOriginalAmount();
        BigDecimal cumulativeAmount = binanceLimitOrder.getCumulativeAmount();
        BigDecimal fee = binanceLimitOrder.getCommissionAmount() == null ? binanceLimitOrder.getFee() : binanceLimitOrder.getCommissionAmount();

        switch (binanceLimitOrder.getType()) {
            case BID -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> Util.debit(counter, originalAmount.multiply(limitPrice), "New Binance Buy Limit order");
                    case FILLED, PARTIALLY_FILLED -> {
                        Util.credit(base, cumulativeAmount, "Filled or Partially Filled Binance Buy Limit order");
                        Util.debit(commissionAsset, fee, "Fee for Filled or Partially Filled Binance Buy Limit order");
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(counter, originalAmount.multiply(limitPrice), "Cancelled Binance Buy Limit order");
                }
            }
            case ASK -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> Util.debit(base, originalAmount, "New Binance Sell Limit order");
                    case FILLED, PARTIALLY_FILLED -> {
                        Util.credit(counter, cumulativeAmount.multiply(limitPrice), "Filled or Partially Filled Binance Sell Limit order");
                        Util.debit(commissionAsset, fee, "Fee for Filled or Partially Filled Binance Sell Limit order");
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(base, originalAmount, "Cancelled Binance Sell Limit order");
                }
            }
            default -> LOG.warn("binanceLimitOrder type unknown: {}", binanceLimitOrder.getType());
        }
    }

    public static void updateBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder, LimitOrder limitOrder) {
        binanceLimitOrder.setType(limitOrder.getType());
        binanceLimitOrder.setOriginalAmount(limitOrder.getOriginalAmount());
        binanceLimitOrder.setInstrument(limitOrder.getInstrument().toString());
        binanceLimitOrder.setId(limitOrder.getId());
        binanceLimitOrder.setTimestamp(limitOrder.getTimestamp());
        binanceLimitOrder.setLimitPrice(limitOrder.getLimitPrice());
        binanceLimitOrder.setAveragePrice(limitOrder.getAveragePrice());
        binanceLimitOrder.setCumulativeAmount(limitOrder.getCumulativeAmount());
        binanceLimitOrder.setFee(limitOrder.getFee());
        binanceLimitOrder.setStatus(limitOrder.getStatus());
        binanceLimitOrder.setUserReference(limitOrder.getUserReference());
        binanceLimitOrder.setCumulativeAmount(limitOrder.getCumulativeCounterAmount());
        binanceLimitOrder.setLeverage(limitOrder.getLeverage());
    }
}