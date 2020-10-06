package com.jessethouin.quant.calculators;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class EMATest {

    @Test
    void ema() {
        int lookback = 4;

        // 11,12,14,18,12,15,13,16,10
        BigDecimal[] data = new BigDecimal[]{
                BigDecimal.valueOf(11),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(14),
                BigDecimal.valueOf(18),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(15),
                BigDecimal.valueOf(13),
                BigDecimal.valueOf(16),
                BigDecimal.valueOf(10)
        };

        // 11.5,11.7,12.62,14.772,13.663,14.198,13.719,14.631
        BigDecimal[] expected = new BigDecimal[]{
                BigDecimal.valueOf(11.400),
                BigDecimal.valueOf(12.440),
                BigDecimal.valueOf(14.664),
                BigDecimal.valueOf(13.598),
                BigDecimal.valueOf(14.159),
                BigDecimal.valueOf(13.695),
                BigDecimal.valueOf(14.617),
                BigDecimal.valueOf(12.770)
        };

        BigDecimal[] results = new BigDecimal[]{};

        BigDecimal previous = BigDecimal.ZERO;
        for (int i = 0; i < data.length; i++) {
            BigDecimal d = data[i];
            if (i == 0) {
                previous = d;
                continue;
            }
            BigDecimal ma = EMA.ema(d, previous, lookback);
            results = ArrayUtils.add(results, ma);
            previous = ma;
        }
        System.out.println(ArrayUtils.toString(results));
    }
}