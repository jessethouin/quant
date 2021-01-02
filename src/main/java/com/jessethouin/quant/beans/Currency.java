package com.jessethouin.quant.beans;

import com.jessethouin.quant.conf.CurrencyTypes;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "CURRENCY", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "portfolio_id"}))
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long currencyId;
    private String symbol;
    private CurrencyTypes currencyType;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "baseCurrency", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<CurrencyPosition> currencyPositions = new HashSet<>();

    public long getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(long currencyId) {
        this.currencyId = currencyId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public CurrencyTypes getCurrencyType() {
        return currencyType;
    }

    public void setCurrencyType(CurrencyTypes currencyType) {
        this.currencyType = currencyType;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public Set<CurrencyPosition> getCurrencyPositions() {
        return currencyPositions;
    }

    public void setCurrencyPositions(Set<CurrencyPosition> currencyPositions) {
        this.currencyPositions = currencyPositions;
    }
}
