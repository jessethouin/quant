package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Security;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderStatus;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Random;

public class AlpacaTestTransactions {
    public static void placeTestSecurityBuyOrder(Security security, BigDecimal qty, BigDecimal price) {
        placeTestSecurityOrder(security, qty, price, OrderSide.BUY);
    }

    public static void placeTestSecuritySellOrder(Security security, BigDecimal qty, BigDecimal price) {
        placeTestSecurityOrder(security, qty, price, OrderSide.SELL);
    }

    private static void placeTestSecurityOrder(Security security, BigDecimal qty, BigDecimal price, OrderSide orderSide) {
        if (qty.equals(BigDecimal.ZERO)) return;
        Order order = getSampleOrder(security, qty, price, orderSide);
        processTestOrder(qty, price, new AlpacaOrder(order, security.getPortfolio()));
    }

    private static Order getSampleOrder(Security security, BigDecimal qty, BigDecimal price, OrderSide orderSide) {
        /*
        * String id,
        * String clientOrderId,
        * ZonedDateTime createdAt,
        * ZonedDateTime updatedAt,
        * ZonedDateTime submittedAt,
        * ZonedDateTime filledAt,
        * ZonedDateTime expiredAt,
        * ZonedDateTime canceledAt,
        * ZonedDateTime failedAt,
        * ZonedDateTime replacedAt,
        * String replacedBy,
        * String replaces,
        * String assetId,
        * String symbol,
        * String assetClass,
        * String notional,
        * String quantity,
        * String filledQuantity,
        * String averageFillPrice,
        * OrderClass orderClass,
        * OrderType type,
        * OrderSide side,
        * OrderTimeInForce timeInForce,
        * String limitPrice,
        * String stopPrice,
        * OrderStatus status,
        * Boolean extendedHours,
        * ArrayList<net.jacobpeterson.alpaca.model.endpoint.orders.Order> legs,
        * String trailPercent,
        * String trailPrice,
        * String highWaterMark
        * */
        return new Order(
                "fake_order_id_" + (new Random().nextInt(1000)),
                "fake_client_order_id_" + (new Random().nextInt(1000)),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                null,
                null,
                null,
                security.getSymbol(),
                null,
                null,
                qty.toString(),
                null,
                null,
                null,
                OrderType.LIMIT,
                orderSide,
                OrderTimeInForce.DAY,
                price.toString(),
                null,
                OrderStatus.NEW,
                false,
                new ArrayList<>(),
                null,
                null,
                null
        );
    }

    private static void processTestOrder(BigDecimal qty, BigDecimal price, AlpacaOrder alpacaOrder) {
        // This code would normally be handled by the Order websocket feed
        if (alpacaOrder == null)
            return;
        alpacaOrder.setStatus(OrderStatus.FILLED);
        alpacaOrder.setFilledAt(ZonedDateTime.now());
        alpacaOrder.setFilledQty(qty.toPlainString());
        alpacaOrder.setFilledAvgPrice(price.toPlainString());
        AlpacaStreamProcessor.processFilledSecurityOrder(alpacaOrder);
    }
}
