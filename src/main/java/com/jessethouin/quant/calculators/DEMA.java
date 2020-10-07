package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DEMA {
    public static BigDecimal dema(BigDecimal price, BigDecimal previousEMA, int lookback) {
        BigDecimal ema = EMA.ema(price, previousEMA, lookback);
        return ema.multiply(BigDecimal.valueOf(2)).subtract(EMA.ema(ema, previousEMA, lookback)).setScale(3, RoundingMode.HALF_UP);
    }
}
