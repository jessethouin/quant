package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import net.jacobpeterson.alpaca.openapi.trader.model.*;
import net.jacobpeterson.alpaca.openapi.trader.model.TimeInForce;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This class is only used in backtesting.
 */
public class AlpacaTestTransactions {
    public static void placeTestCurrencyBuyOrder(Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        placeTestCurrencyOrder(base, counter, qty, price, OrderSide.BUY);
    }

    public static void placeTestCurrencySellOrder(Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        placeTestCurrencyOrder(base, counter, qty, price, OrderSide.SELL);
    }

    private static void placeTestCurrencyOrder(Currency base, Currency counter, BigDecimal qty, BigDecimal price, OrderSide orderSide) {
        if (qty.equals(BigDecimal.ZERO)) return;
        Order order = getSampleOrder(counter.getSymbol() + "/" + base.getSymbol(), qty, price, orderSide, AssetClass.CRYPTO);
        processTestOrder(qty, price, new AlpacaOrder(order, base.getPortfolio()));
    }

    public static void placeTestSecurityBuyOrder(Security security, BigDecimal qty, BigDecimal price) {
        placeTestSecurityOrder(security, qty, price, OrderSide.BUY);
    }

    public static void placeTestSecuritySellOrder(Security security, BigDecimal qty, BigDecimal price) {
        placeTestSecurityOrder(security, qty, price, OrderSide.SELL);
    }

    private static void placeTestSecurityOrder(Security security, BigDecimal qty, BigDecimal price, OrderSide orderSide) {
        if (qty.equals(BigDecimal.ZERO)) return;
        Order order = getSampleOrder(security.getSymbol(), qty, price, orderSide, AssetClass.US_EQUITY);
        processTestOrder(qty, price, new AlpacaOrder(order, security.getPortfolio()));
    }

    private static Order getSampleOrder(String symbol, BigDecimal qty, BigDecimal price, OrderSide orderSide, AssetClass assetClass) {
        Order order = new Order();
        order.setId(UUID.randomUUID().toString());
        order.setClientOrderId(UUID.randomUUID().toString());
        order.setCreatedAt(OffsetDateTime.now());
        order.setUpdatedAt(OffsetDateTime.now());
        order.setSubmittedAt(OffsetDateTime.now());
        order.setSymbol(symbol);
        order.setAssetClass(assetClass);
        order.setQty(qty.toPlainString());
        order.setSide(orderSide);
        order.setType(OrderType.LIMIT);
        order.setTimeInForce(TimeInForce.DAY);
        order.setLimitPrice(price.toPlainString());
        order.setStatus(OrderStatus.NEW);
        order.setExtendedHours(false);
        order.setLegs(new ArrayList<>());
        return order;
    }

    private static void processTestOrder(BigDecimal qty, BigDecimal price, AlpacaOrder alpacaOrder) {
        // This code would normally be handled by the Order websocket feed
        if (alpacaOrder == null)
            return;
        Portfolio portfolio = alpacaOrder.getPortfolio();
        portfolio.getAlpacaOrders().add(alpacaOrder);
        alpacaOrder.setStatus(OrderStatus.FILLED);
        alpacaOrder.setFilledAt(OffsetDateTime.now());
        alpacaOrder.setFilledQty(qty.toPlainString());
        alpacaOrder.setFilledAvgPrice(price.toPlainString());
        switch (alpacaOrder.getAssetClass()) {
            case CRYPTO -> processFilledTestCurrencyOrder(alpacaOrder);
            case US_EQUITY -> processFilledTestSecurityOrder(alpacaOrder);
        }

    }

    private static void processFilledTestCurrencyOrder(AlpacaOrder alpacaOrder) {
        Portfolio portfolio = alpacaOrder.getPortfolio();
        Currency base = Util.getCurrencyFromPortfolio("USD", portfolio);
        Currency counter = Util.getCurrencyFromPortfolio(AlpacaUtil.parseAlpacaCryptoSymbol(alpacaOrder.getSymbol()), portfolio);
        BigDecimal filledQty = new BigDecimal(alpacaOrder.getFilledQty());
        BigDecimal filledAvgPrice = new BigDecimal(alpacaOrder.getFilledAvgPrice());

        if (alpacaOrder.getSide().equals(OrderSide.BUY)) {
            Util.credit(counter, filledQty, "Buying Alpaca Test Currency", alpacaOrder.getId());
            Util.debit(base, filledQty.multiply(filledAvgPrice), "Buying Alpaca Test Currency", alpacaOrder.getId());
        }
        if (alpacaOrder.getSide().equals(OrderSide.SELL)) {
            Util.credit(base, filledQty.multiply(filledAvgPrice), "Selling Alpaca Test Currency", alpacaOrder.getId());
            Util.debit(counter, filledQty, "Selling Alpaca Test Currency", alpacaOrder.getId());
        }
    }

    private static void processFilledTestSecurityOrder(AlpacaOrder alpacaOrder) {
        Portfolio portfolio = alpacaOrder.getPortfolio();
        Security security = Util.getSecurityFromPortfolio(alpacaOrder.getSymbol(), portfolio);
        BigDecimal filledQty = new BigDecimal(alpacaOrder.getFilledQty());
        BigDecimal filledAvgPrice = new BigDecimal(alpacaOrder.getFilledAvgPrice());

        if (alpacaOrder.getSide().equals(OrderSide.BUY)) {
            Transactions.adjustSecurityPosition(security, filledQty, filledAvgPrice);
            Util.debit(security.getCurrency(), filledQty.multiply(filledAvgPrice).negate(), "Buying Alpaca Security", alpacaOrder.getId());
        }
        if (alpacaOrder.getSide().equals(OrderSide.SELL)) {
            Util.credit(security.getCurrency(), filledQty.multiply(filledAvgPrice), "Selling Alpaca Security", alpacaOrder.getId());
            Transactions.adjustSecurityPosition(security, filledQty, filledAvgPrice);
        }
    }
}
