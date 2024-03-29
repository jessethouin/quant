package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EMA {
    public static BigDecimal ema(BigDecimal price, BigDecimal previousEMA, int lookback) {
        double k = 2 / ((double)lookback + 1);
        return price.multiply(BigDecimal.valueOf(k)).add(previousEMA.multiply(BigDecimal.valueOf(1 - k))).setScale(4, RoundingMode.HALF_UP);
    }
}
