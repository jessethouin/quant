package com.jessethouin.quant.binance;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_MIN_TRADES;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_TRADE_SERVICE;
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
            LOG.info("Our Limit Order build (local): " + limitOrder.toString());
            String id = BINANCE_TRADE_SERVICE.placeLimitOrder(limitOrder);
            if (id == null) throw new Exception("Limit Order id was null from server.");
            BinanceStreamProcessing.getOrderHistoryLookup().setOrderId(Long.parseLong(id));
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void processBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder) {
        final Long orderId = binanceLimitOrder.getOrderId();
        Portfolio portfolio = binanceLimitOrder.getPortfolio();
        CurrencyPair currencyPair = new CurrencyPair(binanceLimitOrder.getInstrument());
        Currency base = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        Currency counter = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
        Currency commissionAsset = Objects.requireNonNullElse(binanceLimitOrder.getCommissionAsset(), counter);
        BigDecimal limitPrice = binanceLimitOrder.getLimitPrice();
        BigDecimal bid = binanceLimitOrder.getOriginalAmount().multiply(limitPrice).setScale(8, RoundingMode.HALF_UP);
        BigDecimal ask = binanceLimitOrder.getOriginalAmount();
        BigDecimal cumulativeAmount = binanceLimitOrder.getCumulativeAmount();
        BigDecimal proceeds = cumulativeAmount == null ? null : cumulativeAmount.multiply(limitPrice).setScale(8, RoundingMode.HALF_UP);
        BigDecimal fee = binanceLimitOrder.getCommissionAmount() == null ? binanceLimitOrder.getFee() : binanceLimitOrder.getCommissionAmount();

        switch (binanceLimitOrder.getType()) {
            case BID -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> Util.debit(counter, bid, "New Binance Buy Limit order");
                    case FILLED -> {
                        Util.credit(base, cumulativeAmount, String.format("Filled Binance Buy Limit order %s", orderId));
                        Util.debit(commissionAsset, fee, String.format("Fee for Filled Binance Buy Limit order %s", orderId));
                    }
                    case PARTIALLY_FILLED -> {
                        Util.credit(base, cumulativeAmount, String.format("Partially Filled Binance Buy Limit order %s", orderId));
                        Util.debit(commissionAsset, fee, String.format("Fee for Partially Filled Binance Buy Limit order %s", orderId));
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(counter, bid, String.format("Cancelled Binance Buy Limit order %s", orderId));
                }
            }
            case ASK -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> Util.debit(base, ask, String.format("New Binance Sell Limit order %s", orderId));
                    case FILLED -> {
                        Util.credit(counter, proceeds, String.format("Filled Binance Sell Limit order %s", orderId));
                        Util.debit(commissionAsset, fee, String.format("Fee for Filled Binance Sell Limit order %s", orderId));
                    }
                    case PARTIALLY_FILLED -> {
                        Util.credit(counter, proceeds, String.format("Partially Filled Binance Sell Limit order %s", orderId));
                        Util.debit(commissionAsset, fee, String.format("Fee for Partially Filled Binance Sell Limit order %s", orderId));
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(base, ask, String.format("Cancelled Binance Sell Limit order %s", orderId));
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
        binanceLimitOrder.setLeverage(limitOrder.getLeverage());
    }
}