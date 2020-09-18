package com.jessethouin.quant;

import com.jessethouin.quant.conf.Config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

class Calc {
    private final Security security;

    private BigDecimal price;
    private BigDecimal dema1;
    private BigDecimal dema2;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal spread;
    private boolean buy;
    private BigDecimal qty;

    public Calc(Security security, BigDecimal price, BigDecimal high, BigDecimal low, BigDecimal spread, boolean buy) {
        this.security = security;
        this.price = price;
        this.high = high;
        this.low = low;
        this.spread = spread;
        this.buy = buy;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public Security getSecurity() {
        return security;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDema1() {
        return dema1;
    }

    public void setDema1(BigDecimal dema1) {
        this.dema1 = dema1;
    }

    public BigDecimal getDema2() {
        return dema2;
    }

    public void setDema2(BigDecimal dema2) {
        this.dema2 = dema2;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getSpread() {
        return spread;
    }

    public void setSpread(BigDecimal spread) {
        this.spread = spread;
    }

    public boolean isBuy() {
        return buy;
    }

    public void setBuy(boolean buy) {
        this.buy = buy;
    }

    public BigDecimal decide() {
        BigDecimal proceeds = BigDecimal.ZERO;

        if (price.compareTo(high) > 0) {
            high = price;
        } else if (price.compareTo(low) < 0) {
            low = price;
            high = price;
            spread = high.subtract(low);
            buy = true;
            return proceeds;
        }

        spread = high.subtract(low);

        if (dema1.signum() == 0 || dema2.signum() == 0) return proceeds;

        if (dema1.compareTo(dema2) > 0 && dema1.compareTo(low.add(spread.multiply(Config.getLowRisk()))) > 0 && dema1.compareTo(high.subtract(spread.multiply(Config.getHighRisk()))) < 0 && buy) {
            proceeds = security.buySecurity(qty, price).negate();
            buy = false;
        } else if (dema1.compareTo(dema2) < 0 && price.compareTo(high.subtract(spread.multiply(Config.getHighRisk()))) < 0) {
            proceeds = security.sellSecurity(price);
            buy = true;
        }

        return proceeds;
    }

    public void updateCalc(BigDecimal price, BigDecimal ma1, BigDecimal ma2, Portfolio portfolio) {
        setPrice(price);
        setQty(portfolio.getBudget(price));
        setDema1(ma1);
        setDema2(ma2);
    }
}
