package com.jessethouin.quant.calculators;

import java.math.BigDecimal;

public class MA {
    public static BigDecimal ma(BigDecimal price, BigDecimal previous, int period, MATypes type) {
        BigDecimal ma = BigDecimal.ZERO;
        switch (type) {
            case DEMA -> ma = DEMA.dema(price, previous, period);
            case TEMA -> ma = TEMA.tema(price, previous, period);
        }
        return ma;
    }
}
