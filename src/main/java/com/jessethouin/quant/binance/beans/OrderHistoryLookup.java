package com.jessethouin.quant.binance.beans;

import javax.persistence.*;

@Entity
@Table(name = "ORDER_HISTORY_LOOKUP")
public class OrderHistoryLookup {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
    private long tradeId;
    private long orderId;

    public OrderHistoryLookup() {
    }

    public OrderHistoryLookup(long tradeId, long orderId) {
        this.tradeId = tradeId;
        this.orderId = orderId;
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
}
