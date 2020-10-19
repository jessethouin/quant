package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class TEMA {
    public static BigDecimal tema(BigDecimal price, BigDecimal previousEMA, int lookback) {
        //3 * period - 2
        BigDecimal ema1 = EMA.ema(price, previousEMA, lookback);
        BigDecimal ema2 = EMA.ema(ema1, previousEMA, lookback);
        BigDecimal ema3 = EMA.ema(ema2, previousEMA, lookback);
        return (ema1.multiply(BigDecimal.valueOf(3))).subtract(ema2.multiply(BigDecimal.valueOf(3))).add(ema3).setScale(4, RoundingMode.HALF_UP);
    }
}
