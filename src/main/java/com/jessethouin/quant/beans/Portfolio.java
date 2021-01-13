package com.jessethouin.quant.beans;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "PORTFOLIO")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long portfolioId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private Set<Security> securities = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private Set<Currency> currencies = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private Set<AlpacaOrder> alpacaOrders = new HashSet<>();
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "portfolio", fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private Set<BinanceLimitOrder> binanceLimitOrders = new HashSet<>();
}
