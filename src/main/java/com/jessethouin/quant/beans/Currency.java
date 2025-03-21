package com.jessethouin.quant.beans;

import com.jessethouin.quant.conf.CurrencyType;
import com.jessethouin.quant.db.BigDecimalConverter;
import com.jessethouin.quant.db.Exclude;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CURRENCY", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "portfolio_id"}))
@Getter
@Setter
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long currencyId;
    private String symbol;
    private CurrencyType currencyType;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal quantity;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal avgCostBasis;
    @Exclude
    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "currency", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<CurrencyLedger> currencyLedgers = new HashSet<>();
}
