package com.jessethouin.quant.alpaca.beans;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.conf.AssetClassTypes;
import com.jessethouin.quant.db.Exclude;
import lombok.Getter;
import lombok.Setter;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderStatus;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.Objects;

@Entity
@Table(name = "ALPACA_ORDER")
@Getter
@Setter
public class AlpacaOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;
    @Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    private String id;
    private String clientOrderId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ZonedDateTime submittedAt;
    private ZonedDateTime filledAt;
    private ZonedDateTime expiredAt;
    private ZonedDateTime canceledAt;
    private ZonedDateTime failedAt;
    private ZonedDateTime replacedAt;
    private String replacedBy;
    private String replaces;
    private String assetId;
    private String symbol;
    private AssetClassTypes assetClass;
    private String qty;
    private String filledQty;
    private OrderType type;
    private OrderSide side;
    private OrderTimeInForce timeInForce;
    private String limitPrice;
    private String stopPrice;
    private String filledAvgPrice;
    private OrderStatus status;
    private Boolean extendedHours;
    private String trailPrice;
    private String trailPercent;
    private String highWaterMark;

    public AlpacaOrder() {
    }

    public AlpacaOrder(Order order, Portfolio portfolio) {
        this.portfolio = portfolio;
        this.id = order.getId();
        this.clientOrderId = order.getClientOrderId();
        this.createdAt = order.getCreatedAt();
        this.updatedAt = order.getUpdatedAt();
        this.submittedAt = order.getSubmittedAt();
        this.filledAt = order.getFilledAt();
        this.expiredAt = order.getExpiredAt();
        this.canceledAt = order.getCanceledAt();
        this.failedAt = order.getFailedAt();
        this.replacedAt = order.getReplacedAt();
        this.replacedBy = order.getReplacedBy();
        this.replaces = order.getReplaces();
        this.assetId = order.getAssetId();
        this.symbol = order.getSymbol();
        this.assetClass = AssetClassTypes.get(order.getAssetClass());
        this.qty = order.getQuantity();
        this.filledQty = order.getFilledQuantity();
        this.type = order.getType();
        this.side = order.getSide();
        this.timeInForce = order.getTimeInForce();
        this.limitPrice = order.getLimitPrice();
        this.stopPrice = order.getStopPrice();
        this.filledAvgPrice = order.getAverageFillPrice();
        this.status = order.getStatus();
        this.extendedHours = order.getExtendedHours();
        this.trailPrice = order.getTrailPrice();
        this.trailPercent = order.getTrailPercent();
        this.highWaterMark = order.getHighWaterMark();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(AlpacaOrder.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("id");
        sb.append('=');
        sb.append(this.id == null ? "<null>" : this.id);
        sb.append(',');
        sb.append("clientOrderId");
        sb.append('=');
        sb.append(this.clientOrderId == null ? "<null>" : this.clientOrderId);
        sb.append(',');
        sb.append("createdAt");
        sb.append('=');
        sb.append(this.createdAt == null ? "<null>" : this.createdAt);
        sb.append(',');
        sb.append("updatedAt");
        sb.append('=');
        sb.append(this.updatedAt == null ? "<null>" : this.updatedAt);
        sb.append(',');
        sb.append("submittedAt");
        sb.append('=');
        sb.append(this.submittedAt == null ? "<null>" : this.submittedAt);
        sb.append(',');
        sb.append("filledAt");
        sb.append('=');
        sb.append(this.filledAt == null ? "<null>" : this.filledAt);
        sb.append(',');
        sb.append("expiredAt");
        sb.append('=');
        sb.append(this.expiredAt == null ? "<null>" : this.expiredAt);
        sb.append(',');
        sb.append("canceledAt");
        sb.append('=');
        sb.append(this.canceledAt == null ? "<null>" : this.canceledAt);
        sb.append(',');
        sb.append("failedAt");
        sb.append('=');
        sb.append(this.failedAt == null ? "<null>" : this.failedAt);
        sb.append(',');
        sb.append("replacedAt");
        sb.append('=');
        sb.append(this.replacedAt == null ? "<null>" : this.replacedAt);
        sb.append(',');
        sb.append("replacedBy");
        sb.append('=');
        sb.append(this.replacedBy == null ? "<null>" : this.replacedBy);
        sb.append(',');
        sb.append("replaces");
        sb.append('=');
        sb.append(this.replaces == null ? "<null>" : this.replaces);
        sb.append(',');
        sb.append("assetId");
        sb.append('=');
        sb.append(this.assetId == null ? "<null>" : this.assetId);
        sb.append(',');
        sb.append("symbol");
        sb.append('=');
        sb.append(this.symbol == null ? "<null>" : this.symbol);
        sb.append(',');
        sb.append("assetClass");
        sb.append('=');
        sb.append(this.assetClass == null ? "<null>" : this.assetClass);
        sb.append(',');
        sb.append("qty");
        sb.append('=');
        sb.append(this.qty == null ? "<null>" : this.qty);
        sb.append(',');
        sb.append("filledQty");
        sb.append('=');
        sb.append(this.filledQty == null ? "<null>" : this.filledQty);
        sb.append(',');
        sb.append("type");
        sb.append('=');
        sb.append(this.type == null ? "<null>" : this.type);
        sb.append(',');
        sb.append("side");
        sb.append('=');
        sb.append(this.side == null ? "<null>" : this.side);
        sb.append(',');
        sb.append("timeInForce");
        sb.append('=');
        sb.append(this.timeInForce == null ? "<null>" : this.timeInForce);
        sb.append(',');
        sb.append("limitPrice");
        sb.append('=');
        sb.append(this.limitPrice == null ? "<null>" : this.limitPrice);
        sb.append(',');
        sb.append("stopPrice");
        sb.append('=');
        sb.append(this.stopPrice == null ? "<null>" : this.stopPrice);
        sb.append(',');
        sb.append("filledAvgPrice");
        sb.append('=');
        sb.append(this.filledAvgPrice == null ? "<null>" : this.filledAvgPrice);
        sb.append(',');
        sb.append("status");
        sb.append('=');
        sb.append(this.status == null ? "<null>" : this.status);
        sb.append(',');
        sb.append("extendedHours");
        sb.append('=');
        sb.append(this.extendedHours == null ? "<null>" : this.extendedHours);
        sb.append(',');
        sb.append("trailPrice");
        sb.append('=');
        sb.append(this.trailPrice == null ? "<null>" : this.trailPrice);
        sb.append(',');
        sb.append("trailPercent");
        sb.append('=');
        sb.append(this.trailPercent == null ? "<null>" : this.trailPercent);
        sb.append(',');
        sb.append("hwm");
        sb.append('=');
        sb.append(this.highWaterMark == null ? "<null>" : this.highWaterMark);
        sb.append(',');
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setCharAt(sb.length() - 1, ']');
        } else {
            sb.append(']');
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlpacaOrder that = (AlpacaOrder) o;
        return orderId.equals(that.orderId) &&
                Objects.equals(portfolio, that.portfolio) &&
                Objects.equals(id, that.id) &&
                Objects.equals(clientOrderId, that.clientOrderId) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(submittedAt, that.submittedAt) &&
                Objects.equals(filledAt, that.filledAt) &&
                Objects.equals(expiredAt, that.expiredAt) &&
                Objects.equals(canceledAt, that.canceledAt) &&
                Objects.equals(failedAt, that.failedAt) &&
                Objects.equals(replacedAt, that.replacedAt) &&
                Objects.equals(replacedBy, that.replacedBy) &&
                Objects.equals(replaces, that.replaces) &&
                Objects.equals(assetId, that.assetId) &&
                Objects.equals(symbol, that.symbol) &&
                Objects.equals(assetClass, that.assetClass) &&
                Objects.equals(qty, that.qty) &&
                Objects.equals(filledQty, that.filledQty) &&
                Objects.equals(type, that.type) &&
                Objects.equals(side, that.side) &&
                Objects.equals(timeInForce, that.timeInForce) &&
                Objects.equals(limitPrice, that.limitPrice) &&
                Objects.equals(stopPrice, that.stopPrice) &&
                Objects.equals(filledAvgPrice, that.filledAvgPrice) &&
                Objects.equals(status, that.status) &&
                Objects.equals(extendedHours, that.extendedHours) &&
                Objects.equals(trailPrice, that.trailPrice) &&
                Objects.equals(trailPercent, that.trailPercent) &&
                Objects.equals(highWaterMark, that.highWaterMark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, portfolio, id, clientOrderId, createdAt, updatedAt, submittedAt, filledAt, expiredAt, canceledAt, failedAt, replacedAt, replacedBy, replaces, assetId, symbol, assetClass, qty, filledQty, type, side, timeInForce, limitPrice, stopPrice, filledAvgPrice, status, extendedHours, trailPrice, trailPercent, highWaterMark);
    }
}