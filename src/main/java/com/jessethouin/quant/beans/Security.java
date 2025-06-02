package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.Exclude;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "SECURITY", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "portfolio_id"}))
@Getter
@Setter
public class Security {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long securityId;
    private String symbol;
    @Exclude
    @ManyToOne
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    @Exclude
    @ManyToOne
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "security", fetch = FetchType.EAGER, orphanRemoval = true)
    private SecurityPosition securityPosition = new SecurityPosition();
}
