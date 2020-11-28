package com.jessethouin.quant.binance.beans;

import org.hibernate.annotations.GenericGenerator;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/*
This is esentially an exact copy of org.knowm.xchange.dto.trade.LimitOrder,
but those stick-in-the-muds made their members private, so I couldn't just
extend the damned thing.
*/
@Entity
@Table(name = "BINANCE_LIMIT_ORDER")
public class BinanceLimitOrder implements Comparable<BinanceLimitOrder> {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private long orderId;

    private Order.OrderType type;
    private BigDecimal originalAmount;
    private String instrument;
    private String id;
    private String userReference;
    private Date timestamp;
    private Order.OrderStatus status;
    private BigDecimal cumulativeAmount;
    private BigDecimal averagePrice;
    private BigDecimal fee;
    private String leverage;
    private BigDecimal limitPrice;

    public BinanceLimitOrder(LimitOrder limitOrder) {
        this.type = limitOrder.getType();
        this.originalAmount = limitOrder.getOriginalAmount();
        this.instrument = limitOrder.getInstrument().toString();
        this.id = limitOrder.getId();
        this.timestamp = limitOrder.getTimestamp();
        this.limitPrice = limitOrder.getLimitPrice();
        this.averagePrice = limitOrder.getAveragePrice();
        this.cumulativeAmount = limitOrder.getCumulativeAmount();
        this.fee = limitOrder.getFee();
        this.status = limitOrder.getStatus();
        this.userReference = limitOrder.getUserReference();
        this.cumulativeAmount = limitOrder.getCumulativeCounterAmount();
        this.leverage = limitOrder.getLeverage();
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public Order.OrderType getType() {
        return type;
    }

    public void setType(Order.OrderType type) {
        this.type = type;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserReference() {
        return userReference;
    }

    public void setUserReference(String userReference) {
        this.userReference = userReference;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Order.OrderStatus getStatus() {
        return status;
    }

    public void setStatus(Order.OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getCumulativeAmount() {
        return cumulativeAmount;
    }

    public void setCumulativeAmount(BigDecimal cumulativeAmount) {
        this.cumulativeAmount = cumulativeAmount;
    }

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getLeverage() {
        return leverage;
    }

    public void setLeverage(String leverage) {
        this.leverage = leverage;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    @Override
    public String toString() {
        return "BinanceLimitOrder "
                + "[limitPrice="
                + printLimitPrice()
                + ", type="
                + type
                + ", originalAmount="
                + print(originalAmount)
                + ", cumulativeAmount="
                + print(cumulativeAmount)
                + ", averagePrice="
                + print(averagePrice)
                + ", fee="
                + print(fee)
                + ", instrument="
                + instrument
                + ", id="
                + id
                + ", timestamp="
                + timestamp
                + ", status="
                + status
                + ", userReference="
                + userReference
                + "]";
    }

    @Override
    public int compareTo(BinanceLimitOrder limitOrder) {
        final int ret;
        if (this.getType() == limitOrder.getType()) {
            // Same side
            ret = this.getLimitPrice().compareTo(limitOrder.getLimitPrice()) * (getType() == Order.OrderType.BID ? -1 : 1);
        } else {
            // Keep bid side be less than ask side
            ret = this.getType() == Order.OrderType.BID ? -1 : 1;
        }
        return ret;
    }

    private String printLimitPrice() {
        return limitPrice == null ? null : limitPrice.toPlainString();
    }

    private static String print(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    public BigDecimal getRemainingAmount() {
        if (cumulativeAmount != null && originalAmount != null) {
            return originalAmount.subtract(cumulativeAmount);
        }
        return originalAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinanceLimitOrder that = (BinanceLimitOrder) o;
        return orderId == that.orderId &&
                type == that.type &&
                Objects.equals(originalAmount, that.originalAmount) &&
                Objects.equals(instrument, that.instrument) &&
                Objects.equals(id, that.id) &&
                Objects.equals(userReference, that.userReference) &&
                Objects.equals(timestamp, that.timestamp) &&
                status == that.status &&
                Objects.equals(cumulativeAmount, that.cumulativeAmount) &&
                Objects.equals(averagePrice, that.averagePrice) &&
                Objects.equals(fee, that.fee) &&
                Objects.equals(leverage, that.leverage) &&
                Objects.equals(limitPrice, that.limitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, type, originalAmount, instrument, id, userReference, timestamp, status, cumulativeAmount, averagePrice, fee, leverage, limitPrice);
    }
}
