package com.jessethouin.quant.conf;

import com.jessethouin.quant.calculators.Calc;

public enum SellStrategyTypes {
    SELL1 {
        @Override
        public boolean sell(Calc c) {
            return c.getMa1().compareTo(c.getMa2()) < 0 &&
                c.getPrice().compareTo(c.getLow().add(c.getSpread().multiply(c.getConfig().getHighRisk()))) < 0;
        }
    },
    SELL2 {
        @Override
        public boolean sell(Calc c) {
            return c.getPrice().compareTo(c.getHigh().subtract(c.getSpread().multiply(c.getConfig().getLowRisk()))) < 0;
        }
    },
    SELL3 {
        @Override
        public boolean sell(Calc c) {
            return c.getPrice().compareTo(c.getHigh().subtract(c.getSpread().multiply(c.getConfig().getHighRisk()))) < 0;
        }
    },
    SELL4 {
        @Override
        public boolean sell(Calc c) {
            return c.getMa1().compareTo(c.getMa2()) < 0 &&
                c.getPrice().compareTo(c.getHigh().subtract(c.getSpread().multiply(c.getConfig().getLowRisk()))) < 0;
        }
    },
    SELL5 {
        @Override
        public boolean sell(Calc c) {
            return c.getMa1().compareTo(c.getMa2()) < 0;
        }
    },
    SELL6 {
        @Override
        public boolean sell(Calc c) {
            return !c.isMa1rising() &&
                (c.getMaTrendDiff().compareTo(c.getMaFallingTrendTolerance()) < 0 || c.getMaTrend() < c.getMaFallingCountTolerance()) &&
                !c.isBuy();
        }
    };

    public abstract boolean sell(Calc calc);
}
