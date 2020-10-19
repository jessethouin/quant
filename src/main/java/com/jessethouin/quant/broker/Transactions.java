package com.jessethouin.quant.broker;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Position;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.calculators.SMA;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.MATypes;
import com.jessethouin.quant.exceptions.CashException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Transactions {
    private static final Logger LOG = LogManager.getLogger(Transactions.class);

    public static void processStreamMessage(Security s, Config config) {
        final Calc c = new Calc(s, config, BigDecimal.ZERO);
        final List<BigDecimal> intradayPrices = new ArrayList<>();
        int count = 0;
        BigDecimal shortMAValue;
        BigDecimal longMAValue;
        BigDecimal price = BigDecimal.ZERO;
        BigDecimal previousShortMAValue = BigDecimal.ZERO;
        BigDecimal previousLongMAValue = BigDecimal.ZERO;

    }

    public static BigDecimal buySecurity(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO)) return BigDecimal.ZERO;
        Position position;
        Optional<Position> existingPosition = security.getPositions().stream().filter(p -> p.getPrice().compareTo(price) == 0).findFirst();

        if (existingPosition.isPresent()) {
            position = existingPosition.get();
            position.setQuantity(position.getQuantity().add(qty));
        } else {
            position = new Position();
            position.setQuantity(qty);
            position.setPrice(price);
            position.setOpened(new Date());
            position.setSecurity(security);
            security.getPositions().add(position);
        }

        LOG.trace("Bought " + qty + " at " + price);
        return qty.multiply(price).setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal sellSecurity(Security security, BigDecimal price, boolean sellAll) {
        List<Position> remove = new ArrayList<>();
        AtomicReference<BigDecimal> cash = new AtomicReference<>(BigDecimal.ZERO);

        security.getPositions().forEach(position -> {
            if (position.getPrice().compareTo(price) <= 0 || sellAll) {
                cash.set(cash.get().add(price.multiply(position.getQuantity())).setScale(4, RoundingMode.HALF_UP));
                remove.add(position);
                LOG.trace("Sold " + position.getQuantity() + " at " + price);
            }
        });

        security.getPositions().removeAll(remove);
        return cash.get();
    }

    public static BigDecimal sellAll(Security security, BigDecimal price) {
        return sellSecurity(security, price, true);
    }

    public static BigDecimal sellSecurity(Security security, BigDecimal price) {
        return sellSecurity(security, price, false);
    }

    public static void addCash(Portfolio portfolio, BigDecimal cash) throws CashException {
        if (cash.signum() < 0) {
            LOG.trace("Deducting cash: " + cash);
            deductCash(portfolio, cash.abs());
            return;
        }
        portfolio.setCash(portfolio.getCash().add(cash));
    }

    public static void deductCash(Portfolio portfolio, BigDecimal cash) throws CashException {
        if (cash.compareTo(portfolio.getCash()) > 0) {
            throw new CashException(String.format("You don't have enough cash to deduct %s. Your balance is %s.", cash, portfolio.getCash()));
        }
        portfolio.setCash(portfolio.getCash().subtract(cash));
    }

    public static BigDecimal getPortfolioValue(Portfolio portfolio, String symbol, BigDecimal price) { //todo: refactor to take Map<String, BigDecimal>
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        portfolio.getSecurities().stream().filter(s -> s.getSymbol().equals(symbol)).forEach(security -> security.getPositions().forEach(position -> holdings.updateAndGet(v -> v.add(price.multiply(position.getQuantity())))));
        return portfolio.getCash().add(holdings.get());
    }

    public static BigDecimal getBudget(Portfolio portfolio, BigDecimal price, BigDecimal allowance) {
        return portfolio.getCash().multiply(allowance).divide(price, 0, RoundingMode.FLOOR);
    }

    public static BigDecimal getMA(List<BigDecimal> intradayPrices, BigDecimal previousMA, int i, int lookback, BigDecimal price) {
        BigDecimal ma;
        if (i < lookback) ma = BigDecimal.ZERO;
        else if (i == lookback) ma = SMA.sma(intradayPrices.subList(0, i), lookback);
        else ma = MA.ma(price, previousMA, lookback, MATypes.TEMA);
        return ma;
    }
}
