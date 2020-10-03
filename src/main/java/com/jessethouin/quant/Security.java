package com.jessethouin.quant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class Security {
    private static final Logger LOG = LogManager.getLogger(Security.class);
    public final String symbol;
    public final HashMap<BigDecimal, BigDecimal> positions = new HashMap<>();

    public Security(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal buySecurity(BigDecimal qty, BigDecimal price) {
        BigDecimal posQty = positions.getOrDefault(price, BigDecimal.ZERO).add(qty);
        positions.put(price, posQty);
        LOG.trace("Bought " + qty + " at " + price);
        return qty.multiply(price).setScale(3, RoundingMode.HALF_UP);
    }

    public BigDecimal sellSecurity(BigDecimal price) {
        List<BigDecimal> remove = new ArrayList<>();
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal q;

        for (BigDecimal p : positions.keySet()) {
            if (p.compareTo(price) <= 0) {
                q = positions.get(p);
                cash = cash.add(price.multiply(q)).setScale(3, RoundingMode.HALF_UP);
                remove.add(p);
                LOG.trace("Sold " + q + " at " + price);
            }
        }

        positions.keySet().removeAll(remove);
        return cash;
    }
}
