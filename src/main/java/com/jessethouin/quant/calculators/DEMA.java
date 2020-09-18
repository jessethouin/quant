package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DEMA {
    public static BigDecimal dema(BigDecimal price, BigDecimal previousEMA, int period) {
        BigDecimal ema = EMA.ema(price, previousEMA, period);
        return ema.multiply(BigDecimal.valueOf(2)).subtract(EMA.ema(ema, previousEMA, period)).setScale(3, RoundingMode.HALF_UP);
    }
}
