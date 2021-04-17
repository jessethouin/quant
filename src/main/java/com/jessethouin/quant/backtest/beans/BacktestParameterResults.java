package com.jessethouin.quant.backtest.beans;

import com.jessethouin.quant.conf.BuyStrategyTypes;
import com.jessethouin.quant.conf.SellStrategyTypes;
import com.jessethouin.quant.db.BigDecimalConverter;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "BACKTEST_PARAMETER_RESULTS")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class BacktestParameterResults {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long backtestId;
    private Date timestamp;
    private Date start;
    private Date end;
    BuyStrategyTypes buyStrategyType;
    SellStrategyTypes sellStrategyType;
    int shortLookback;
    int longLookback;
    @Convert(converter = BigDecimalConverter.class)
    BigDecimal allowance;
    @Convert(converter = BigDecimalConverter.class)
    BigDecimal stopLoss;
    @Convert(converter = BigDecimalConverter.class)
    BigDecimal lowRisk;
    @Convert(converter = BigDecimalConverter.class)
    BigDecimal highRisk;
    @Convert(converter = BigDecimalConverter.class)
    BigDecimal bids;
    @Convert(converter = BigDecimalConverter.class)
    BigDecimal value;
}
