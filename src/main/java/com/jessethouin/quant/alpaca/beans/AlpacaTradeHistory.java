package com.jessethouin.quant.alpaca.beans;

import com.jessethouin.quant.db.BigDecimalConverter;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ALPACA_TRADE_HISTORY")
public class AlpacaTradeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long alpacaTradeHistoryId;
    private String ticker; //Ticker of the object
    private Long t; //Nanosecond accuracy SIP Unix Timestamp
    private Long y; //Nanosecond accuracy Participant/Exchange Unix Timestamp
    private Long f; //Nanosecond accuracy TRF(Trade Reporting Facility) Unix Timestamp
    private Integer q; //Sequence Number
    private String i; //Trade ID
    private Integer x; //Exchange ID
    private Integer s; //Size/Volume of the trade
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal p; //Price of the trade
    private Integer z; //Tape where trade occured. ( 1,2 = CTA, 3 = UTP )

    public Long getAlpacaTradeHistoryId() {
        return alpacaTradeHistoryId;
    }

    public void setAlpacaTradeHistoryId(Long alpacaTradeHistoryId) {
        this.alpacaTradeHistoryId = alpacaTradeHistoryId;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public Long getT() {
        return t;
    }

    public void setT(Long t) {
        this.t = t;
    }

    public Long getY() {
        return y;
    }

    public void setY(Long y) {
        this.y = y;
    }

    public Long getF() {
        return f;
    }

    public void setF(Long f) {
        this.f = f;
    }

    public Integer getQ() {
        return q;
    }

    public void setQ(Integer q) {
        this.q = q;
    }

    public String getI() {
        return i;
    }

    public void setI(String i) {
        this.i = i;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getS() {
        return s;
    }

    public void setS(Integer s) {
        this.s = s;
    }

    public BigDecimal getP() {
        return p;
    }

    public void setP(BigDecimal p) {
        this.p = p;
    }

    public Integer getZ() {
        return z;
    }

    public void setZ(Integer z) {
        this.z = z;
    }
}
