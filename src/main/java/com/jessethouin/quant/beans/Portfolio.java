package com.jessethouin.quant.beans;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PORTFOLIO")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long portfolioId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Security> securities = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Currency> currencies = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<AlpacaOrder> alpacaOrders = new HashSet<>();

    public Long getPortfolioId() {
        return portfolioId;
    }

    public void setPortfolioId(long portfolioId) {
        this.portfolioId = portfolioId;
    }

    public Set<Security> getSecurities() {
        return securities;
    }

    public void setSecurities(Set<Security> securities) {
        this.securities = securities;
    }

    public Set<Currency> getCurrencies() {
        return currencies;
    }

    public void setCurrencies(Set<Currency> currencies) {
        this.currencies = currencies;
    }

    public Set<AlpacaOrder> getAlpacaOrders() {
        return alpacaOrders;
    }

    public void setAlpacaOrders(Set<AlpacaOrder> alpacaOrders) {
        this.alpacaOrders = alpacaOrders;
    }
}
