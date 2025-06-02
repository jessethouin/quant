package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import jakarta.persistence.*;
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
    private Long id;
    private long tradeId;
    private String orderId;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal value;
}
