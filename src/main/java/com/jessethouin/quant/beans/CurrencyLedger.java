package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "CURRENCY_LEDGER")
@Getter
@Setter
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

    public CurrencyLedger(Currency currency, BigDecimal debit, BigDecimal credit, Date timestamp) {
        this.currency = currency;
        this.debit = debit;
        this.credit = credit;
        this.timestamp = timestamp;
    }

    public CurrencyLedger() {}

    public static class Builder {
        private Currency currency;
        private BigDecimal debit;
        private BigDecimal credit;
        private Date timestamp;

        public Builder setCurrency(Currency currency) {
            this.currency = currency;
            return this;
        }

        public Builder setDebit(BigDecimal debit) {
            this.debit = debit;
            return this;
        }

        public Builder setCredit(BigDecimal credit) {
            this.credit = credit;
            return this;
        }

        public Builder setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public CurrencyLedger build() {
            return new CurrencyLedger(currency, debit, credit, timestamp);
        }
    }
}
