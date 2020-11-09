package com.jessethouin.quant.alpaca.beans;

import net.jacobpeterson.domain.alpaca.order.Order;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ALPACA_ORDER")
public class AlpacaOrder extends Order {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private long orderId;

    public AlpacaOrder(Order order) {
        super(order.getId(), order.getClientOrderId(), order.getCreatedAt(), order.getUpdatedAt(), order.getSubmittedAt(), order.getFilledAt(), order.getExpiredAt(), order.getCanceledAt(), order.getFailedAt(), order.getReplacedAt(),
                order.getReplacedBy(), order.getReplaces(), order.getAssetId(), order.getSymbol(), order.getAssetClass(), order.getQty(), order.getFilledQty(), order.getType(), order.getSide(),
                order.getTimeInForce(), order.getLimitPrice(), order.getStopPrice(), order.getFilledAvgPrice(), order.getStatus(), order.getExtendedHours(), order.getLegs(), order.getTrailPrice(),
                order.getTrailPercent(), order.getHwm());
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }
}
