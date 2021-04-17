package com.jessethouin.quant.binance.beans;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.db.BigDecimalConverter;
import com.jessethouin.quant.db.Exclude;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;

/*
This is esentially an exact copy of org.knowm.xchange.dto.trade.LimitOrder,
but those stick-in-the-muds made their members private, so I couldn't just
extend the damned thing.
*/
@Entity
@Table(name = "BINANCE_LIMIT_ORDER")
@Getter
@Setter
public class BinanceLimitOrder implements Comparable<BinanceLimitOrder> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    @Exclude
    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    private Order.OrderType type;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal originalAmount;
    private String instrument;
    private String id;
    private String userReference;
    private Date timestamp;
    private Order.OrderStatus status;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal cumulativeAmount;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal averagePrice;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal fee;
    private String leverage;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal limitPrice;
    @ManyToOne
    @JoinColumn(name = "commission_currency_id")
    private Currency commissionAsset;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal commissionAmount;

    public BinanceLimitOrder() {
    }

    public BinanceLimitOrder(LimitOrder limitOrder, Portfolio portfolio) {
        this.portfolio = portfolio;
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
        this.leverage = limitOrder.getLeverage();
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
                + ", commissionAsset="
                + commissionAsset
                + ", commissionAmount="
                + commissionAmount
                + "]";
    }

    private String printLimitPrice() {
        return limitPrice == null ? null : limitPrice.toPlainString();
    }

    private static String print(BigDecimal value) {
        return value == null ? null : value.toPlainString();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinanceLimitOrder that = (BinanceLimitOrder) o;
        return orderId.equals(that.orderId) &&
                Objects.equals(portfolio, that.portfolio) &&
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
        return Objects.hash(orderId, portfolio, type, originalAmount, instrument, id, userReference, timestamp, status, cumulativeAmount, averagePrice, fee, leverage, limitPrice);
    }
}
