package com.jessethouin.quant.calculators;

import com.jessethouin.quant.conf.MATypes;

import java.math.BigDecimal;

public class MA {
    public static BigDecimal ma(BigDecimal price, BigDecimal previousMA, int lookback, MATypes type) {
        BigDecimal ma = BigDecimal.ZERO;
        switch (type) {
            case EMA -> ma = EMA.ema(price, previousMA, lookback);
            case DEMA -> ma = DEMA.dema(price, previousMA, lookback);
            case TEMA -> ma = TEMA.tema(price, previousMA, lookback);
        }
        return ma;
    }
}
