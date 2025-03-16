package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.alpaca.beans.repos.AlpacaOrderRepository;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Security;
import net.jacobpeterson.alpaca.openapi.trader.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_ORDERS_API;

@Component
public class AlpacaTransactions {
    private static final Logger LOG = LogManager.getLogger(AlpacaTransactions.class);
    public static AlpacaOrderRepository alpacaOrderRepository;

    public AlpacaTransactions(AlpacaOrderRepository alpacaOrderRepository) {
        AlpacaTransactions.alpacaOrderRepository = alpacaOrderRepository;
    }

    public static void buyCurrency(Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        transact(null, base, counter, qty, price, OrderSide.BUY);
    }

    public static void sellCurrency(Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        transact(null, base, counter, qty, price, OrderSide.SELL);
    }

    public static void buySecurity(Security security, BigDecimal qty, BigDecimal price) {
        transact(security, null, null, qty, price, OrderSide.BUY);
    }

    public static void sellSecurity(Security security, BigDecimal qty, BigDecimal price) {
        transact(security, null, null, qty, price, OrderSide.SELL);
    }

    private static void transact(Security security, Currency base, Currency counter, BigDecimal qty, BigDecimal price, OrderSide orderSide) {
        if (qty.compareTo(BigDecimal.ZERO) == 0 || qty.equals(BigDecimal.ZERO)) return;

        String symbol;

        if (security == null) {
            symbol = counter.getSymbol() + "/" + base.getSymbol();
        } else {
            symbol = security.getSymbol();
        }

        try {
            PostOrderRequest postOrderRequest = new PostOrderRequest();
            postOrderRequest.setType(OrderType.LIMIT);
            postOrderRequest.setSymbol(symbol);
            postOrderRequest.setQty(qty.toPlainString());
            postOrderRequest.setSide(orderSide);
            postOrderRequest.setTimeInForce(TimeInForce.DAY);
            postOrderRequest.setLimitPrice(price.toPlainString());
            postOrderRequest.setExtendedHours(false);
            Order order = ALPACA_ORDERS_API.postOrder(postOrderRequest);
            if (order.getId() == null) throw new Exception("Limit Order id was null from server.");
            LOG.info("New {} order: {}", orderSide, order.toString().replace(",", ",\n\t"));
            AlpacaStreamProcessor.getOrderHistoryLookup().setOrderId(order.getId());
            return;
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }
        LOG.error("{} order failed: {}, {}, {}", orderSide, symbol, qty, price);
    }

    public static void updateAlpacaOrder(AlpacaOrder alpacaOrder, Order order) {
        alpacaOrder.setId(order.getId());
        alpacaOrder.setClientOrderId(order.getClientOrderId());
        alpacaOrder.setCreatedAt(order.getCreatedAt());
        alpacaOrder.setUpdatedAt(order.getUpdatedAt());
        alpacaOrder.setSubmittedAt(order.getSubmittedAt());
        alpacaOrder.setFilledAt(order.getFilledAt());
        alpacaOrder.setExpiredAt(order.getExpiredAt());
        alpacaOrder.setCanceledAt(order.getCanceledAt());
        alpacaOrder.setFailedAt(order.getFailedAt());
        alpacaOrder.setReplacedAt(order.getReplacedAt());
        alpacaOrder.setReplacedBy(order.getReplacedBy());
        alpacaOrder.setReplaces(order.getReplaces());
        alpacaOrder.setAssetId(order.getAssetId());
        alpacaOrder.setSymbol(order.getSymbol());
        alpacaOrder.setAssetClass(order.getAssetClass());
        alpacaOrder.setQty(order.getQty());
        alpacaOrder.setFilledQty(order.getFilledQty());
        alpacaOrder.setType(order.getType());
        alpacaOrder.setSide(order.getSide());
        alpacaOrder.setTimeInForce(order.getTimeInForce());
        alpacaOrder.setLimitPrice(order.getLimitPrice());
        alpacaOrder.setStopPrice(order.getStopPrice());
        alpacaOrder.setFilledAvgPrice(order.getFilledAvgPrice());
        alpacaOrder.setStatus(order.getStatus());
        alpacaOrder.setExtendedHours(order.getExtendedHours());
        alpacaOrder.setTrailPrice(order.getTrailPrice());
        alpacaOrder.setTrailPercent(order.getTrailPercent());
        alpacaOrder.setHighWaterMark(order.getHwm());
        alpacaOrderRepository.save(alpacaOrder);
    }
}
