package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "SECURITY_POSITION")
public class SecurityPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long positionId;
    private Date opened;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal quantity;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal price;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "security_id")
    private Security security;

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(long positionId) {
        this.positionId = positionId;
    }

    public Date getOpened() {
        return opened;
    }

    public void setOpened(Date opened) {
        this.opened = opened;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }
}
