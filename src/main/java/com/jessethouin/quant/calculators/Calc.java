package com.jessethouin.quant.calculators;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

public class Calc {
    private static final Logger LOG = LogManager.getLogger(Calc.class);

    private final Security security;
    private final Currency base;
    private final Currency counter;
    private final Config config;

    private BigDecimal price;
    private BigDecimal ma1;
    private BigDecimal ma2;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal spread;
    private boolean buy;
    private BigDecimal qty;

    public Calc(Security security, Config config, BigDecimal price) {
        this(security, security.getCurrency(), null, config, price, price, price, BigDecimal.ZERO, true);
    }

    public Calc(Currency base, Currency counter, Config config, BigDecimal price) {
        this(null, base, counter, config, price, price, price, BigDecimal.ZERO, true);
    }

    public Calc(Security security, Currency base, Currency counter, Config config, BigDecimal price, BigDecimal high, BigDecimal low, BigDecimal spread, boolean buy) {
        this.security = security;
        this.base = base;
        this.counter = counter;
        this.config = config;
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

    public Currency getBase() {
        return base;
    }

    public Currency getCounter() {
        return counter;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getMa1() {
        return ma1;
    }

    public void setMa1(BigDecimal ma1) {
        this.ma1 = ma1;
    }

    public BigDecimal getMa2() {
        return ma2;
    }

    public void setMa2(BigDecimal ma2) {
        this.ma2 = ma2;
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

    public void decide() {
        if (getPrice().compareTo(getHigh()) > 0) {
            setHigh(getPrice());
        } else if (getPrice().compareTo(getLow()) < 0) {
            setLow(getPrice());
            setHigh(getPrice());
            setSpread(BigDecimal.ZERO);
            setBuy(true);
            return;
        }

        setSpread(getHigh().subtract(getLow()));

        if (getMa1().signum() == 0 || getMa2().signum() == 0) return;

        boolean buy = switch (config.getBuyStrategy()) {
            case BUY1 -> buy1();
            case BUY2 -> buy2();
            case BUY3 -> buy3();
        };

        boolean sell = switch (config.getSellStrategy()) {
            case SELL1 -> sell1();
            case SELL2 -> sell2();
            case SELL3 -> sell3();
            case SELL4 -> sell4();
        };

        if (buy) {
            Transactions.placeBuyOrder(config.getBroker(), getSecurity(), getBase(), getCounter(), getQty(), getPrice());
            setBuy(false);
        } else if (sell) {
            boolean success = Transactions.placeSellOrder(config.getBroker(), getSecurity(), getBase(), getCounter(), getPrice());
            if (success) {
                setLow(price);
                setHigh(price);
            }
            setBuy(true);
        }
    }

    private boolean buy1() {
        return getMa1().compareTo(getMa2()) > 0 &&
                getMa1().compareTo(getLow().add(getSpread().multiply(config.getLowRisk()))) > 0 &&
                getMa1().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0 &&
                isBuy();
    }

    public boolean buy2() {
        return getPrice().compareTo(getLow().add(getSpread().multiply(config.getLowRisk()))) > 0 &&
                getPrice().compareTo(getLow().add(getSpread().multiply(config.getLowRisk().multiply(BigDecimal.valueOf(3))))) < 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0 &&
                isBuy();
    }

    private boolean buy3() {
        return getMa1().compareTo(getLow().add(getSpread().multiply(config.getLowRisk()))) > 0 &&
                getMa1().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0 &&
                isBuy();
    }

    public boolean sell1() {
        return getMa1().compareTo(getMa2()) < 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0;
    }

    public boolean sell2() {
        if (getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))) < 0) {
            LOG.info("Sell2 met. Here's why.\n" +
                    "\nconfig.getLowRisk() = " + config.getLowRisk() +
                    "\ngetSpread() = " + getSpread() +
                    "\ngetSpread().multiply(config.getLowRisk()) = " + getSpread().multiply(config.getLowRisk()) +
                    "\ngetHigh() = " + getHigh() +
                    "\ngetHigh().subtract(getSpread().multiply(config.getLowRisk())) = " + getHigh().subtract(getSpread().multiply(config.getLowRisk())) +
                    "\ngetPrice() = " + getPrice() +
                    "\ngetPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))) = " + getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))));
            return true;
        } else {
            return false;
        }
//        return getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))) < 0;
    }

    public boolean sell3() {
        return getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0;
    }

    public boolean sell4() {
        return getMa1().compareTo(getMa2()) < 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))) < 0;
    }

    public void updateCalc(BigDecimal price, BigDecimal ma1, BigDecimal ma2, Portfolio portfolio) {
        setPrice(price);
        if (getLow().equals(BigDecimal.ZERO)) setLow(price);
        if (getHigh().equals(BigDecimal.ZERO)) setHigh(price);
        setQty(Util.getBudget(portfolio, price, config.getAllowance(), getBase()));
        setMa1(ma1);
        setMa2(ma2);
    }
}
