package com.jessethouin.quant.calculators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MA {
    public static List<BigDecimal> ma(List<BigDecimal> in, int period, MATypes type) {
        List<BigDecimal> output = new ArrayList<>();
        BigDecimal previous = BigDecimal.ZERO;
        for (int i = 0; i < in.size(); i++) {
            if (i < period) {
                output.add(BigDecimal.ZERO);
                continue;
            }
            if (i == period) {
                previous = SMA.sma(in.subList(0, period), period);
                output.add(BigDecimal.ZERO);
                continue;
            }
            switch (type) {
                case DEMA -> previous = DEMA.dema(in.get(i), previous, period);
                case TEMA -> previous = TEMA.tema(in.get(i), previous, period);
            }
            output.add(previous);
        }
        return output;
    }
}
