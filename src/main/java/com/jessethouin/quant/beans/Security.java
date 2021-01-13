package com.jessethouin.quant.beans;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "SECURITY", uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "portfolio_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Security {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long securityId;
    private String symbol;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "currency_id")
    private Currency currency;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "security", fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private Set<SecurityPosition> securityPositions = new HashSet<>();
}
