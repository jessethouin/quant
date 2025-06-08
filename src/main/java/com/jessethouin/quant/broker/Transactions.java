package com.jessethouin.quant.broker;

import com.jessethouin.quant.alpaca.AlpacaTestTransactions;
import com.jessethouin.quant.alpaca.AlpacaTransactions;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.SecurityPosition;
import com.jessethouin.quant.conf.Broker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

public class Transactions {
    private static final Logger LOG = LogManager.getLogger(Transactions.class);

    public static void placeBuyOrder(Broker broker, Security security, Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO) || qty.compareTo(BigDecimal.ZERO) == 0) return;

        if (security != null) {
            price = price.setScale(2, RoundingMode.HALF_UP);
            placeSecurityBuyOrder(broker, security, qty, price);
        } else {
            placeCurrencyBuyOrder(broker, base, counter, qty, price);
        }
    }

    public static boolean placeSellOrder(Broker broker, Security security, Currency base, Currency counter, BigDecimal price) {
        if (security != null) {
            return placeSecuritySellOrder(broker, security, price);
        } else {
            return placeCurrencySellOrder(broker, base, counter, price);
        }
    }

    private static void placeCurrencyBuyOrder(Broker broker, Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + base.getSymbol() + " at " + price);

        switch (broker) {
            case ALPACA -> {
                LOG.debug("Placing Alpaca BUY LIMIT order for {} of {} at {}", qty.toPlainString(), counter.getSymbol(), price);
                AlpacaTransactions.buyCurrency(base, counter, qty, price);
            }
            case ALPACA_CRYPTO_TEST -> {
                LOG.debug("Placing Alpaca BUY LIMIT order for {} of {} at {}", qty.toPlainString(), counter.getSymbol(), price);
                AlpacaTestTransactions.placeTestCurrencyBuyOrder(base, counter, qty, price);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    private static boolean placeCurrencySellOrder(Broker broker, Currency base, Currency counter, BigDecimal price) {
        if (base.getQuantity().compareTo(BigDecimal.ZERO) == 0) return false;

        switch (broker) {
            case ALPACA -> {
                LOG.debug("Placing Alpaca SELL LIMIT order for {} of {} at {}", counter.getQuantity().toPlainString(), counter.getSymbol() + "/" + base.getSymbol(), price);
                AlpacaTransactions.sellCurrency(base, counter, counter.getQuantity(), price);
            }
            case ALPACA_CRYPTO_TEST -> {
                LOG.debug("Placing Alpaca SELL LIMIT order for {} of {} at {}", counter.getQuantity().toPlainString(), counter.getSymbol() + "/" + base.getSymbol(), price);
                AlpacaTestTransactions.placeTestCurrencySellOrder(base, counter, counter.getQuantity(), price);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }

    private static void placeSecurityBuyOrder(Broker broker, Security security, BigDecimal qty, BigDecimal price) {
        LOG.debug("Create buy order for {} {} at {}", qty, security.getSymbol(), price);

        switch (broker) {
            case ALPACA -> AlpacaTransactions.buySecurity(security, qty, price);
            case ALPACA_SECURITY_TEST -> AlpacaTestTransactions.placeTestSecurityBuyOrder(security, qty, price);
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    private static boolean placeSecuritySellOrder(Broker broker, Security security, BigDecimal price) {
        BigDecimal sellQty = security.getSecurityPosition().getQuantity();
        if (sellQty.equals(BigDecimal.ZERO)) return false;

        LOG.debug("Create sell order for {} {} at {}", sellQty, security.getSymbol(), price);

        switch (broker) {
            case ALPACA -> AlpacaTransactions.sellSecurity(security, sellQty, price);
            case ALPACA_SECURITY_TEST -> AlpacaTestTransactions.placeTestSecuritySellOrder(security, sellQty, price);
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }

    public static void adjustSecurityPosition(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.compareTo(BigDecimal.ZERO) == 0) return;

        SecurityPosition securityPosition = security.getSecurityPosition();
        if (securityPosition == null) {
            if (qty.compareTo(BigDecimal.ZERO) > 0) {
                securityPosition = new SecurityPosition();
                securityPosition.setQuantity(qty);
                securityPosition.setPrice(price);
                securityPosition.setSecurity(security);
                securityPosition.setOpened(new Date());
                security.setSecurityPosition(securityPosition);
            }
        } else {
            BigDecimal currentQty = securityPosition.getQuantity();
            BigDecimal currentPrice = securityPosition.getPrice();
            BigDecimal newPrice = BigDecimal.ZERO;
            if (currentQty.add(qty).compareTo(BigDecimal.ZERO) > 0) {
                // ((current price * current quantity) + (new price * new quantity)) / (current quantity + new quantity)
                newPrice = ((currentPrice.multiply(currentQty)).add(price.multiply(qty))).divide(currentQty.add(qty), RoundingMode.HALF_UP);
            }
            securityPosition.setQuantity(securityPosition.getQuantity().add(qty));
            securityPosition.setPrice(newPrice);
        }
    }
}
