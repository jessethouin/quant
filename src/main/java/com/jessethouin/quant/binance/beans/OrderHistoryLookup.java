package com.jessethouin.quant.binance.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ORDER_HISTORY_LOOKUP")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryLookup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private long tradeId;
    private long orderId;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal value;
}
