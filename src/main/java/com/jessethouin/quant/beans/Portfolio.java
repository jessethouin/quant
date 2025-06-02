package com.jessethouin.quant.beans;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import lombok.*;

import jakarta.persistence.*;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PORTFOLIO")
@Getter
@Setter
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long portfolioId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Security> securities = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<Currency> currencies = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<AlpacaOrder> alpacaOrders = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<BinanceLimitOrder> binanceLimitOrders = new HashSet<>();
}
