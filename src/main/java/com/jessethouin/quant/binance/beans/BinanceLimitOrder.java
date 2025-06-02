package com.jessethouin.quant.binance.beans;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.db.BigDecimalConverter;
import com.jessethouin.quant.db.Exclude;
import lombok.Getter;
import lombok.Setter;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/*
This is essentially an exact copy of org.knowm.xchange.dto.trade.LimitOrder,
but those sticks-in-the-mud made their members private, so I couldn't just
extend the damned thing.
*/
@Entity
@Table(name = "BINANCE_LIMIT_ORDER")
@Getter
@Setter
public class BinanceLimitOrder {
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
                + print(limitPrice)
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
                + (commissionAsset == null ? null : commissionAsset.getSymbol())
                + ", commissionAmount="
                + print(commissionAmount)
                + "]";
    }

    private static String print(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
