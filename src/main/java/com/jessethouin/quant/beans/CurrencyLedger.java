package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import com.jessethouin.quant.db.Exclude;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "CURRENCY_LEDGER")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long currencyLedgerId;
    private Date timestamp;
    private String orderId;
    private String memo;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal debit;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal credit;
    @Exclude
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
}
