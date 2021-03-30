package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.alpaca.beans.repos.AlpacaOrderRepository;
import net.jacobpeterson.domain.alpaca.order.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Component
public class AlpacaUtil {
    public static AlpacaOrderRepository alpacaOrderRepository;

    public AlpacaUtil(AlpacaOrderRepository alpacaOrderRepository) {
        AlpacaUtil.alpacaOrderRepository = alpacaOrderRepository;
    }

    public static void updateAlpacaOrder(AlpacaOrder alpacaOrder, Order order) {
        alpacaOrder.setClientOrderId(order.getClientOrderId());
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
        alpacaOrder.setHwm(order.getHwm());
        alpacaOrderRepository.save(alpacaOrder);
    }

}
