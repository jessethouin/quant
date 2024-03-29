package com.jessethouin.quant.conf;

import com.jessethouin.quant.calculators.Calc;
import java.math.BigDecimal;

public enum BuyStrategyType {
    BUY1 {
        @Override
        public boolean buy(Calc c) {
            return c.getMa1().compareTo(c.getMa2()) > 0 &&
                c.getPrice().compareTo(c.getHigh().subtract(c.getSpread().multiply(c.getConfig().getHighRisk()))) < 0 &&
                c.isBuy();
        }
    },
    BUY2 {
        @Override
        public boolean buy(Calc c) {
            return c.getPrice().compareTo(c.getLow().add(c.getSpread().multiply(c.getConfig().getLowRisk()))) > 0 &&
                c.getPrice().compareTo(c.getLow().add(c.getSpread().multiply(c.getConfig().getLowRisk().multiply(BigDecimal.valueOf(3))))) < 0 &&
                c.getPrice().compareTo(c.getHigh().subtract(c.getSpread().multiply(c.getConfig().getHighRisk()))) < 0 &&
                c.isBuy();
        }
    },
    BUY3 {
        @Override
        public boolean buy(Calc c) {
            return c.getPrice().compareTo(c.getLow().add(c.getSpread().multiply(c.getConfig().getLowRisk()))) > 0 &&
                c.getPrice().compareTo(c.getHigh().subtract(c.getSpread().multiply(c.getConfig().getHighRisk()))) < 0 &&
                c.isBuy();
        }
    },
    BUY4 {
        @Override
        public boolean buy(Calc c) {
            return c.getMa1().compareTo(c.getMa2()) > 0 &&
                c.isBuy();
        }
    };

    public abstract boolean buy(Calc calc);
}
