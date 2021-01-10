package com.jessethouin.quant.binance.beans;

import com.jessethouin.quant.db.BigDecimalConverter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_HISTORY_LOOKUP")
public class OrderHistoryLookup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long tradeId;
    private long orderId;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal value;

    public OrderHistoryLookup() {
    }

    public OrderHistoryLookup(long tradeId, long orderId, BigDecimal value) {
        this.tradeId = tradeId;
        this.orderId = orderId;
        this.value = value;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getTradeId() {
        return tradeId;
    }

    public void setTradeId(long tradeId) {
        this.tradeId = tradeId;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
