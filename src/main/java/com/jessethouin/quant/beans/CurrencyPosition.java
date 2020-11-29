package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "CURRENCY_POSITION")
public class CurrencyPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long positionId;
    private Date opened;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal quantity;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal price;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "counter_currency_id")
    private Currency counterCurrency;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id")
    private Currency baseCurrency;

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

    public Currency getCounterCurrency() {
        return counterCurrency;
    }

    public void setCounterCurrency(Currency counterCurrency) {
        this.counterCurrency = counterCurrency;
    }

    public Currency getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(Currency baseCurrency) {
        this.baseCurrency = baseCurrency;
    }
}
