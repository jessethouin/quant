package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.Exclude;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "security", fetch = FetchType.EAGER, orphanRemoval = true)
    private Set<SecurityPosition> securityPositions = new HashSet<>();
}
