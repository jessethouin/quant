package com.jessethouin.quant;

import com.jessethouin.quant.exceptions.CashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class Portfolio {
    private static final Logger LOG = LogManager.getLogger(Portfolio.class);
    private BigDecimal cash = BigDecimal.ZERO;
    private ArrayList<Security> securities = new ArrayList<>();

    public ArrayList<Security> getSecurities() {
        return securities;
    }

    public void setSecurities(ArrayList<Security> securities) {
        this.securities = securities;
    }

    public BigDecimal getCash() {
        return cash;
    }

    public void setCash(BigDecimal cash) {
        this.cash = cash;
    }

    public void addCash(BigDecimal cash) throws CashException {
        if (cash.signum() < 0) {
            deductCash(cash.abs());
            return;
        }
        this.cash = this.cash.add(cash);
    }

    public void deductCash(BigDecimal cash) throws CashException {
        if (cash.compareTo(this.cash) > 0) {
            throw new CashException(String.format("You don't have enough cash to deduct %s. Your balance is %s.", cash, this.cash));
        }
        this.cash = this.cash.subtract(cash);
    }

    public String toString() {
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        StringBuilder s = new StringBuilder();
        s.append(System.lineSeparator());
        s.append("Cash: ");
        s.append(getCash());
        s.append(System.lineSeparator());

        securities.forEach(security -> {
            s.append("Security: ");
            s.append(security.symbol);
            s.append(System.lineSeparator());
            security.positions.forEach((p, q) -> {
                s.append("\tPrice: ");
                s.append(p);
                s.append(", Quantity: ");
                s.append(q);
                s.append(", Value: ");
                s.append(p.multiply(q));
                s.append(System.lineSeparator());
                holdings.updateAndGet(v -> v.add(p.multiply(q)));
            });
        });

        s.append("Total value: ");
        s.append(getCash().add(holdings.get()));
        return s.toString();
    }

    public BigDecimal getPortfolioValue() {
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        securities.forEach(security -> security.positions.forEach((p, q) -> holdings.updateAndGet(v -> v.add(p.multiply(q)))));
        return getCash().add(holdings.get());
    }

    public BigDecimal getBudget(BigDecimal price, BigDecimal allowance) {
        return getCash().multiply(allowance).divide(price, 0, RoundingMode.FLOOR);
    }
}
