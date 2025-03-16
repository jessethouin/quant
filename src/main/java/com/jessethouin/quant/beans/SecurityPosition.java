package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import com.jessethouin.quant.db.Exclude;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "SECURITY_POSITION")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityPosition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long positionId;
    private Date opened;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal quantity;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal price;
    @Exclude
    @OneToOne
    @JoinColumn(name = "security_id")
    private Security security;
}
