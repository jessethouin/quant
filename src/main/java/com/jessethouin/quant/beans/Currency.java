package com.jessethouin.quant.beans;

import com.jessethouin.quant.conf.CurrencyTypes;
import com.jessethouin.quant.db.BigDecimalConverter;
import com.jessethouin.quant.db.Exclude;
import lombok.*;

import javax.persistence.*;
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
    private CurrencyTypes currencyType;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal quantity;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal avgCostBais;
    @Exclude
    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "currency", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<CurrencyLedger> currencyLedgers = new HashSet<>();
}
