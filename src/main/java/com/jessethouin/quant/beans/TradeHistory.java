package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "TRADE_HISTORY")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistory {
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
