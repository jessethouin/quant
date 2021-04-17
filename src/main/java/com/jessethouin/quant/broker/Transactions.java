package com.jessethouin.quant.broker;

import com.jessethouin.quant.alpaca.AlpacaTransactions;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.SecurityPosition;
import com.jessethouin.quant.binance.BinanceTestTransactions;
import com.jessethouin.quant.binance.BinanceTransactions;
import com.jessethouin.quant.conf.Broker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.util.Date;

public class Transactions {
    private static final Logger LOG = LogManager.getLogger(Transactions.class);

    public static void placeBuyOrder(Broker broker, Security security, Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        if (qty.equals(BigDecimal.ZERO) || qty.compareTo(BigDecimal.ZERO) == 0) return;

        if (security != null) {
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
            case COINBASE -> LOG.info("Place COINBASE buy order here");
            case CEXIO -> LOG.info("Place CEXIO buy order here");
            case BINANCE -> {
                LOG.info("Placing Binance BUY LIMIT order for {} of {} at {}", qty.toPlainString(), base.getSymbol(), price);
                BinanceTransactions.buyCurrency(new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty, price);
            }
            case BINANCE_TEST -> {
                LOG.debug("Placing Binance TEST BUY LIMIT order for {} of {} at {}", qty.toPlainString(), base.getSymbol(), price);
                BinanceTestTransactions.buyTestCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty, price);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    private static boolean placeCurrencySellOrder(Broker broker, Currency base, Currency counter, BigDecimal price) {
        if (base.getQuantity().compareTo(BigDecimal.ZERO) == 0) return false;

        switch (broker) {
            case COINBASE -> LOG.info("Place COINBASE sell order here");
            case CEXIO -> LOG.info("Place CEXIO sell order here");
            case BINANCE -> {
                LOG.info("Placing Binance SELL LIMIT Order for {} of {} at {}", base.getQuantity().toPlainString(), base.getSymbol(), price);
                BinanceTransactions.sellCurrency(new CurrencyPair(base.getSymbol(), counter.getSymbol()), base.getQuantity(), price);
            }
            case BINANCE_TEST -> {
                LOG.debug("Placing Binance TEST SELL LIMIT Order for {} of {} at {}", base.getQuantity().toPlainString(), base.getSymbol(), price);
                BinanceTestTransactions.sellTestCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), base.getQuantity(), price);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }

    private static void placeSecurityBuyOrder(Broker broker, Security security, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + security.getSymbol() + " at " + price);

        switch (broker) {
            case ALPACA -> AlpacaTransactions.placeSecurityBuyOrder(security, qty, price);
            case ALPACA_TEST -> AlpacaTransactions.placeTestSecurityBuyOrder(security, qty, price);
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    private static boolean placeSecuritySellOrder(Broker broker, Security security, BigDecimal price) {
        BigDecimal[] qty = {BigDecimal.ZERO};

        security.getSecurityPositions().forEach(position -> {
            if (position.getPrice().compareTo(price) < 0) {
                LOG.trace("Create sell order for " + position.getQuantity() + " " + security.getSymbol() + " at " + price);
                qty[0] = qty[0].add(position.getQuantity());
            }
        });

        BigDecimal sellQty = qty[0];
        if (sellQty.equals(BigDecimal.ZERO)) return false;

        switch (broker) {
            case ALPACA -> AlpacaTransactions.placeSecuritySellOrder(security, sellQty, price);
            case ALPACA_TEST -> AlpacaTransactions.placeTestSecuritySellOrder(security, sellQty, price);
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }


    public static void addSecurityPosition(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.compareTo(BigDecimal.ZERO) == 0) return;

        SecurityPosition securityPosition = new SecurityPosition();
        securityPosition.setQuantity(qty);
        securityPosition.setPrice(price);
        securityPosition.setSecurity(security);
        securityPosition.setOpened(new Date());
        security.getSecurityPositions().add(securityPosition);
    }
}
