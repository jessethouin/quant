package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.SecurityPosition;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.enums.OrderSide;
import net.jacobpeterson.alpaca.enums.OrderStatus;
import net.jacobpeterson.alpaca.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.enums.OrderType;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.domain.alpaca.order.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AlpacaTransactions {
    private static final Logger LOG = LogManager.getLogger(AlpacaTransactions.class);

    public static void placeSecurityBuyOrder(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO)) return;
        AlpacaAPI alpacaAPI = AlpacaLive.getAlpacaApi();
        try {
            Order order = alpacaAPI.requestNewLimitOrder(security.getSymbol(), qty.intValue(), OrderSide.BUY, OrderTimeInForce.DAY, price.doubleValue(), false);
            AlpacaOrder alpacaOrder = new AlpacaOrder(order, security.getPortfolio());
            LOG.info("Buy order: " + alpacaOrder.toString().replace(",", ",\n\t"));
            return;
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e.getLocalizedMessage());
        }
        LOG.error("Buy order failed: " + security.getSymbol() + ", " + qty + ", " + price);
    }

    public static void placeSecuritySellOrder(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO)) return;

        AlpacaAPI alpacaAPI = AlpacaLive.getAlpacaApi();
        try {
            Order order = alpacaAPI.requestNewLimitOrder(security.getSymbol(), qty.intValue(), OrderSide.SELL, OrderTimeInForce.DAY, price.doubleValue(), false);
            LOG.info("Sell order: " + order.toString().replace(",", ",\n\t"));
            return;
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e.getLocalizedMessage());
        }
        LOG.info("Sell order failed: " + security.getSymbol() + ", " + qty + ", " + price);
    }

    public static void placeTestSecurityBuyOrder(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO)) return;
        Order order = new Order(
                "fake_order_id_" + (new Random().nextInt(1000)),
                "fake_client_order_id_" + (new Random().nextInt(1000)),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                String.valueOf(security.getSecurityId()),
                security.getSymbol(),
                null,
                qty.toString(),
                "0",
                OrderType.LIMIT.getAPIName(),
                OrderSide.BUY.getAPIName(),
                OrderTimeInForce.DAY.getAPIName(),
                price.toString(),
                null,
                null,
                OrderStatus.OPEN.getAPIName(),
                false,
                new ArrayList<>(),
                null,
                null,
                null
        );
        processTestOrder(qty, price, new AlpacaOrder(order, security.getPortfolio()));
    }

    public static void placeTestSecuritySellOrder(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO)) return;
        Order order = new Order(
                "fake_order_id_" + (new Random().nextInt(1000)),
                "fake_client_order_id_" + (new Random().nextInt(1000)),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                String.valueOf(security.getSecurityId()),
                security.getSymbol(),
                null,
                qty.toString(),
                "0",
                OrderType.LIMIT.getAPIName(),
                OrderSide.SELL.getAPIName(),
                OrderTimeInForce.DAY.getAPIName(),
                price.toString(),
                null,
                null,
                OrderStatus.OPEN.getAPIName(),
                false,
                new ArrayList<>(),
                null,
                null,
                null
        );

        processTestOrder(qty, price, new AlpacaOrder(order, security.getPortfolio()));
    }

    private static void processTestOrder(BigDecimal qty, BigDecimal price, AlpacaOrder alpacaOrder) {
        // This code would normally be handled by the Order websocket feed
        if (alpacaOrder == null)
            return;
        alpacaOrder.setStatus(org.knowm.xchange.dto.Order.OrderStatus.FILLED.toString());
        alpacaOrder.setFilledAt(ZonedDateTime.now());
        alpacaOrder.setFilledQty(qty.toPlainString());
        alpacaOrder.setFilledAvgPrice(price.toPlainString());
        processFilledOrder(alpacaOrder);
    }

    public static void processFilledOrder(AlpacaOrder alpacaOrder) {
        Portfolio portfolio = alpacaOrder.getPortfolio();
        Security security = Util.getSecurityFromPortfolio(alpacaOrder.getSymbol(), portfolio);
        BigDecimal filledQty = new BigDecimal(alpacaOrder.getFilledQty());
        BigDecimal filledAvgPrice = new BigDecimal(alpacaOrder.getFilledAvgPrice());

        if (alpacaOrder.getSide().equals(OrderSide.BUY.getAPIName())) {
            Transactions.addSecurityPosition(security, filledQty, filledAvgPrice);
            Util.debit(security.getCurrency(), filledQty.multiply(filledAvgPrice).negate());
        }
        if (alpacaOrder.getSide().equals(OrderSide.SELL.getAPIName())) {
            Util.credit(security.getCurrency(), filledQty.multiply(filledAvgPrice));

            List<SecurityPosition> remove = new ArrayList<>();

            security.getSecurityPositions().forEach(position -> {
                if (position.getPrice().compareTo(filledAvgPrice) < 0) {
                    remove.add(position);
                }
            });

            BigDecimal[] qty = {filledQty};
            remove.sort(Comparator.comparing(SecurityPosition::getPrice).reversed());

            remove.forEach(securityPosition -> {
                BigDecimal securityPositionQuantity = securityPosition.getQuantity();

                if (securityPositionQuantity.compareTo(qty[0]) <= 0) {
                    security.getSecurityPositions().remove(securityPosition);
                    qty[0] = qty[0].subtract(securityPositionQuantity);
                } else {
                    securityPosition.setQuantity(securityPositionQuantity.subtract(qty[0]));
                    qty[0] = BigDecimal.ZERO;
                }

            });
        }
    }
}
