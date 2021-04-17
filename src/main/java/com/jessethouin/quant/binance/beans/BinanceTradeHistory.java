package com.jessethouin.quant.binance.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "BINANCE_TRADE_HISTORY")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BinanceTradeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;
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
}
