package com.jessethouin.quant.calculators;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Getter
@Setter
public class Calc {

    private static final Logger LOG = LogManager.getLogger(Calc.class);

    private final Security security;
    private final Config config;

    private Currency base;
    private Currency counter;
    private BigDecimal price;
    private BigDecimal ma1 = BigDecimal.ZERO;
    private BigDecimal ma2;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal spread = BigDecimal.ZERO;
    private boolean buy = false;
    private BigDecimal qty;
    private boolean ma1rising;
    private int maTrend;
    private List<BigDecimal> maTrendList = new ArrayList<>();
    private int maRisingCountTolerance = 5;
    private int maFallingCountTolerance = -5;
    private BigDecimal maRisingTrendTolerance = BigDecimal.valueOf(1.000001);
    private BigDecimal maFallingTrendTolerance = BigDecimal.valueOf(0.999997);
    private BigDecimal maTrendRate;
    private BigDecimal maTrendDiff;
    private List<BigDecimal> maTrendRateList = new ArrayList<>();

    public Calc(Security security, Config config, BigDecimal price) {
        this(security, security.getCurrency(), security.getCurrency(), config, price, price, price);
    }

    public Calc(Currency base, Currency counter, Config config, BigDecimal price) {
        this(null, base, counter, config, price, price, price);
    }

    private Calc(Security security, Currency base, Currency counter, Config config, BigDecimal price, BigDecimal high, BigDecimal low) {
        this.security = security;
        this.base = base;
        this.counter = counter;
        this.config = config;
        this.price = price;
        this.high = high;
        this.low = low;
    }

    public void updateCalc(BigDecimal price, BigDecimal ma1, BigDecimal ma2) {
        setPrice(price);
        if (getLow().equals(BigDecimal.ZERO)) {
            setLow(price);
        }
        if (getHigh().equals(BigDecimal.ZERO)) {
            setHigh(price);
        }
        if (getMa1().equals(BigDecimal.ZERO)) {
            setMa1(ma1);
        }
        setQty(Util.getBudget(price, config.getAllowance(), getCounter(), getSecurity()));
        if (ma1.compareTo(getMa1()) >= 0) {
            setMa1rising(true);
            if (maTrend < 0) {
                maTrend = 0;
                maTrendList.clear();
            }
            maTrend++;
        } else {
            setMa1rising(false);
            if (maTrend > 0) {
                maTrend = 0;
                maTrendList.clear();
            }
            maTrend--;
        }
        maTrendDiff = ma1.divide(getMa1(), 16, RoundingMode.HALF_UP);
        maTrendList.add(maTrendDiff);
        maTrendRate = maTrendList.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(maTrendList.size()), RoundingMode.HALF_UP);
        maTrendRateList.add(maTrendRate);
        setMa1(ma1);
        setMa2(ma2);
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

        if (getMa1().signum() == 0 || getMa2().signum() == 0) {
            return;
        }

        setSpread(getHigh().subtract(getLow()));

        boolean buy = config.getBuyStrategy().buy(this);
        boolean sell = config.getSellStrategy().sell(this);

        if (buy || config.isTriggerBuy()) {
            LOG.debug("BID maTrend {} maTrendRate {}", maTrend, maTrendRate);
            LOG.debug("    maTrendList {}", Arrays.toString(maTrendList.toArray()));
            config.setTriggerBuy(false);
            Transactions.placeBuyOrder(config.getBroker(), getSecurity(), getBase(), getCounter(), getQty(), getPrice());
            setBuy(false);
        } else if (sell || config.isTriggerSell()) {
            LOG.debug("ASK maTrend {} maTrendRate {}", maTrend, maTrendRate);
            LOG.debug("    maTrendList {}", Arrays.toString(maTrendList.toArray()));
            config.setTriggerSell(false);
            boolean success = Transactions.placeSellOrder(config.getBroker(), getSecurity(), getBase(), getCounter(), getPrice());
            if (success) {
                setLow(getPrice());
                setHigh(getPrice());
            }
            setBuy(true);
        }
    }
}
