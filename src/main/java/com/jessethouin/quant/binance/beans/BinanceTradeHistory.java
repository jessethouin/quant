package com.jessethouin.quant.binance.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "BINANCE_TRADE_HISTORY")
@Data
public class BinanceTradeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long tradeId;
    private Date timestamp;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal ma1;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal ma2;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal l;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal h;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal p;

    public BinanceTradeHistory() {
    }

    public BinanceTradeHistory(Date timestamp, BigDecimal ma1, BigDecimal ma2, BigDecimal l, BigDecimal h, BigDecimal p) {
        this.timestamp = timestamp;
        this.ma1 = ma1;
        this.ma2 = ma2;
        this.l = l;
        this.h = h;
        this.p = p;
    }

    public static class Builder {
        private Date timestamp;
        private BigDecimal ma1;
        private BigDecimal ma2;
        private BigDecimal l;
        private BigDecimal h;
        private BigDecimal p;

        public Builder setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setMa1(BigDecimal ma1) {
            this.ma1 = ma1;
            return this;
        }

        public Builder setMa2(BigDecimal ma2) {
            this.ma2 = ma2;
            return this;
        }

        public Builder setL(BigDecimal l) {
            this.l = l;
            return this;
        }

        public Builder setH(BigDecimal h) {
            this.h = h;
            return this;
        }

        public Builder setP(BigDecimal p) {
            this.p = p;
            return this;
        }
        
        public BinanceTradeHistory build() {
            return new BinanceTradeHistory(timestamp, ma1, ma2, l, h, p);
        }
    }
}
