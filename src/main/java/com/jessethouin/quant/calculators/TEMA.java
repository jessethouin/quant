package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TEMA {
    public static BigDecimal tema(BigDecimal price, BigDecimal previousEMA, int period) {
        //3 * period - 2
        BigDecimal ema1 = EMA.ema(price, previousEMA, period);
        BigDecimal ema2 = EMA.ema(ema1, previousEMA, period);
        BigDecimal ema3 = EMA.ema(ema2, previousEMA, period);
        return (ema1.multiply(BigDecimal.valueOf(3))).subtract(ema2.multiply(BigDecimal.valueOf(3))).add(ema3).setScale(3, RoundingMode.HALF_UP);
    }
}
