package com.jessethouin.quant.alpaca.beans;

import com.jessethouin.quant.beans.Portfolio;
import net.jacobpeterson.domain.alpaca.order.Order;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Objects;

@Entity
@Table(name = "ALPACA_ORDER")
public class AlpacaOrder {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private long orderId;
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
    private String assetClass;
    private String qty;
    private String filledQty;
    private String type;
    private String side;
    private String timeInForce;
    private String limitPrice;
    private String stopPrice;
    private String filledAvgPrice;
    private String status;
    private Boolean extendedHours;
    private ArrayList<Order> legs;
    private String trailPrice;
    private String trailPercent;
    private String hwm;

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
        this.assetClass = order.getAssetClass();
        this.qty = order.getQty();
        this.filledQty = order.getFilledQty();
        this.type = order.getType();
        this.side = order.getSide();
        this.timeInForce = order.getTimeInForce();
        this.limitPrice = order.getLimitPrice();
        this.stopPrice = order.getStopPrice();
        this.filledAvgPrice = order.getFilledAvgPrice();
        this.status = order.getStatus();
        this.extendedHours = order.getExtendedHours();
        this.legs = order.getLegs();
        this.trailPrice = order.getTrailPrice();
        this.trailPercent = order.getTrailPercent();
        this.hwm = order.getHwm();
    }

    public AlpacaOrder(String id, String clientOrderId, ZonedDateTime createdAt, ZonedDateTime updatedAt, ZonedDateTime submittedAt, ZonedDateTime filledAt, ZonedDateTime expiredAt, ZonedDateTime canceledAt, ZonedDateTime failedAt, ZonedDateTime replacedAt, String replacedBy, String replaces, String assetId, String symbol, String assetClass, String qty, String filledQty, String type, String side, String timeInForce, String limitPrice, String stopPrice, String filledAvgPrice, String status, Boolean extendedHours, ArrayList<Order> legs, String trailPrice, String trailPercent, String hwm) {
        this.id = id;
        this.clientOrderId = clientOrderId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.submittedAt = submittedAt;
        this.filledAt = filledAt;
        this.expiredAt = expiredAt;
        this.canceledAt = canceledAt;
        this.failedAt = failedAt;
        this.replacedAt = replacedAt;
        this.replacedBy = replacedBy;
        this.replaces = replaces;
        this.assetId = assetId;
        this.symbol = symbol;
        this.assetClass = assetClass;
        this.qty = qty;
        this.filledQty = filledQty;
        this.type = type;
        this.side = side;
        this.timeInForce = timeInForce;
        this.limitPrice = limitPrice;
        this.stopPrice = stopPrice;
        this.filledAvgPrice = filledAvgPrice;
        this.status = status;
        this.extendedHours = extendedHours;
        this.legs = legs;
        this.trailPrice = trailPrice;
        this.trailPercent = trailPercent;
        this.hwm = hwm;
    }

    public long getOrderId() {
        return orderId;
    }

    public void setOrderId(long orderId) {
        this.orderId = orderId;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ZonedDateTime getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(ZonedDateTime filledAt) {
        this.filledAt = filledAt;
    }

    public ZonedDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(ZonedDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public ZonedDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(ZonedDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public ZonedDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(ZonedDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public ZonedDateTime getReplacedAt() {
        return replacedAt;
    }

    public void setReplacedAt(ZonedDateTime replacedAt) {
        this.replacedAt = replacedAt;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }

    public String getReplaces() {
        return replaces;
    }

    public void setReplaces(String replaces) {
        this.replaces = replaces;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAssetClass() {
        return assetClass;
    }

    public void setAssetClass(String assetClass) {
        this.assetClass = assetClass;
    }

    public String getQty() {
        return qty;
    }

    public void setQty(String qty) {
        this.qty = qty;
    }

    public String getFilledQty() {
        return filledQty;
    }

    public void setFilledQty(String filledQty) {
        this.filledQty = filledQty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }

    public String getLimitPrice() {
        return limitPrice;
    }

    public void setLimitPrice(String limitPrice) {
        this.limitPrice = limitPrice;
    }

    public String getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(String stopPrice) {
        this.stopPrice = stopPrice;
    }

    public String getFilledAvgPrice() {
        return filledAvgPrice;
    }

    public void setFilledAvgPrice(String filledAvgPrice) {
        this.filledAvgPrice = filledAvgPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getExtendedHours() {
        return extendedHours;
    }

    public void setExtendedHours(Boolean extendedHours) {
        this.extendedHours = extendedHours;
    }

    public ArrayList<Order> getLegs() {
        return legs;
    }

    public void setLegs(ArrayList<Order> legs) {
        this.legs = legs;
    }

    public String getTrailPrice() {
        return trailPrice;
    }

    public void setTrailPrice(String trailPrice) {
        this.trailPrice = trailPrice;
    }

    public String getTrailPercent() {
        return trailPercent;
    }

    public void setTrailPercent(String trailPercent) {
        this.trailPercent = trailPercent;
    }

    public String getHwm() {
        return hwm;
    }

    public void setHwm(String hwm) {
        this.hwm = hwm;
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
        sb.append("legs");
        sb.append('=');
        sb.append(this.legs == null ? "<null>" : this.legs);
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
        sb.append(this.hwm == null ? "<null>" : this.hwm);
        sb.append(',');
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setCharAt(sb.length() - 1, ']');
        } else {
            sb.append(']');
        }

        return sb.toString();
    }

    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof AlpacaOrder)) {
            return false;
        } else {
            AlpacaOrder rhs = (AlpacaOrder) other;
            return Objects.equals(this.symbol, rhs.symbol) &&
                    Objects.equals(this.replacedAt, rhs.replacedAt) &&
                    Objects.equals(this.extendedHours, rhs.extendedHours) &&
                    Objects.equals(this.trailPrice, rhs.trailPrice) &&
                    Objects.equals(this.assetClass, rhs.assetClass) &&
                    Objects.equals(this.type, rhs.type) &&
                    Objects.equals(this.createdAt, rhs.createdAt) &&
                    Objects.equals(this.expiredAt, rhs.expiredAt) &&
                    Objects.equals(this.failedAt, rhs.failedAt) &&
                    Objects.equals(this.assetId, rhs.assetId) &&
                    Objects.equals(this.legs, rhs.legs) &&
                    Objects.equals(this.id, rhs.id) &&
                    Objects.equals(this.submittedAt, rhs.submittedAt) &&
                    Objects.equals(this.timeInForce, rhs.timeInForce) &&
                    Objects.equals(this.updatedAt, rhs.updatedAt) &&
                    Objects.equals(this.side, rhs.side) &&
                    Objects.equals(this.limitPrice, rhs.limitPrice) &&
                    Objects.equals(this.trailPercent, rhs.trailPercent) &&
                    Objects.equals(this.replacedBy, rhs.replacedBy) &&
                    Objects.equals(this.replaces, rhs.replaces) &&
                    Objects.equals(this.clientOrderId, rhs.clientOrderId) &&
                    Objects.equals(this.filledAt, rhs.filledAt) &&
                    Objects.equals(this.filledAvgPrice, rhs.filledAvgPrice) &&
                    Objects.equals(this.stopPrice, rhs.stopPrice) &&
                    Objects.equals(this.canceledAt, rhs.canceledAt) &&
                    Objects.equals(this.qty, rhs.qty) &&
                    Objects.equals(this.filledQty, rhs.filledQty) &&
                    Objects.equals(this.status, rhs.status) &&
                    Objects.equals(this.hwm, rhs.hwm);
        }
    }
}