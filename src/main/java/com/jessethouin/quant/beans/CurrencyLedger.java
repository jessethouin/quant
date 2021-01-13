package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
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
    private long currencyLedgerId;
    private Date timestamp;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal debit;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal credit;
    @ManyToOne(fetch = FetchType.EAGER)
    private Currency currency;
}