package com.jessethouin.quant.calculators;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

@Getter
@Setter
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
        this(security, security.getCurrency(), security.getCurrency(), config, price, price, price, BigDecimal.ZERO, false);
    }

    public Calc(Currency base, Currency counter, Config config, BigDecimal price) {
        this(null, base, counter, config, price, price, price, BigDecimal.ZERO, false);
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

        if (getMa1().signum() == 0 || getMa2().signum() == 0) return;

        setSpread(getHigh().subtract(getLow()));

        boolean buy = switch (config.getBuyStrategy()) {
            case BUY1 -> buy1();
            case BUY2 -> buy2();
            case BUY3 -> buy3();
            case BUY4 -> buy4();
        };

        boolean sell = switch (config.getSellStrategy()) {
            case SELL1 -> sell1();
            case SELL2 -> sell2();
            case SELL3 -> sell3();
            case SELL4 -> sell4();
            case SELL5 -> sell5();
        };

        if (buy) {
            Transactions.placeBuyOrder(config.getBroker(), getSecurity(), getBase(), getCounter(), getQty(), getPrice());
            setBuy(false);
        } else if (sell) {
            boolean success = Transactions.placeSellOrder(config.getBroker(), getSecurity(), getBase(), getCounter(), getPrice());
            if (success) {
                setLow(getPrice());
                setHigh(getPrice());
            }
            setBuy(true);
        }
    }

    private boolean buy1() {
        return getMa1().compareTo(getMa2()) > 0 &&
                getPrice().compareTo(getLow().add(getSpread().multiply(config.getLowRisk()))) > 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0 &&
                isBuy();
    }

    private boolean buy2() {
        return getPrice().compareTo(getLow().add(getSpread().multiply(config.getLowRisk()))) > 0 &&
                getPrice().compareTo(getLow().add(getSpread().multiply(config.getLowRisk().multiply(BigDecimal.valueOf(3))))) < 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0 &&
                isBuy();
    }

    private boolean buy3() {
        return getPrice().compareTo(getLow().add(getSpread().multiply(config.getLowRisk()))) > 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0 &&
                isBuy();
    }

    private boolean buy4() {
        return getMa1().compareTo(getMa2()) > 0 &&
                isBuy();
    }

    private boolean sell1() {
        return getMa1().compareTo(getMa2()) < 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0;
    }

    private boolean sell2() {
        return getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))) < 0;
    }

    private boolean sell3() {
        return getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getHighRisk()))) < 0;
    }

    private boolean sell4() {
        return getMa1().compareTo(getMa2()) < 0 &&
                getPrice().compareTo(getHigh().subtract(getSpread().multiply(config.getLowRisk()))) < 0;
    }

    private boolean sell5() {
        return getMa1().compareTo(getMa2()) < 0;
    }

    public void updateCalc(BigDecimal price, BigDecimal ma1, BigDecimal ma2) {
        setPrice(price);
        if (getLow().equals(BigDecimal.ZERO)) setLow(price);
        if (getHigh().equals(BigDecimal.ZERO)) setHigh(price);
        setQty(Util.getBudget(price, config.getAllowance(), getCounter(), getSecurity()));
        setMa1(ma1);
        setMa2(ma2);
    }
}
