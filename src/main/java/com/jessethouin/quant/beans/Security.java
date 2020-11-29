package com.jessethouin.quant.beans;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "SECURITY", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "portfolio_id"}))
public class Security {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long securityId;
    private String symbol;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "security", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<SecurityPosition> securityPositions = new HashSet<>();

    public Long getSecurityId() {
        return securityId;
    }

    public void setSecurityId(long securityId) {
        this.securityId = securityId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Set<SecurityPosition> getSecurityPositions() {
        return securityPositions;
    }

    public void setSecurityPositions(Set<SecurityPosition> securityPositions) {
        this.securityPositions = securityPositions;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }
}
