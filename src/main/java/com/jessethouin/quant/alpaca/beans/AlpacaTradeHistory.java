package com.jessethouin.quant.alpaca.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ALPACA_TRADE_HISTORY")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlpacaTradeHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
}
