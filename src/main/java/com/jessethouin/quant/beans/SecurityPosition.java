package com.jessethouin.quant.beans;

import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
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
    private long positionId;
    private Date opened;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal quantity;
    @Convert(converter = BigDecimalConverter.class)
    private BigDecimal price;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "security_id")
    private Security security;
}
