package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class SMA {
    public static BigDecimal sma(List<BigDecimal> prices, int period) {
        return prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(period), 3, RoundingMode.HALF_UP);
    }
}
