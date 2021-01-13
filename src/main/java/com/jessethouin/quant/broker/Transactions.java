package com.jessethouin.quant.broker;

import com.jessethouin.quant.alpaca.AlpacaTransactions;
import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.SecurityPosition;
import com.jessethouin.quant.binance.BinanceLive;
import com.jessethouin.quant.binance.BinanceTransactions;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.conf.Broker;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
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

    public static void placeCurrencyBuyOrder(Broker broker, Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + base.getSymbol() + " at " + price);

        switch (broker) {
            case COINBASE -> LOG.info("Place COINBASE buy order here");
            case CEXIO -> LOG.info("Place CEXIO buy order here");
            case BINANCE -> LOG.info("Placing Binance BUY order for {} of {} at {}", qty, base.getSymbol(), price);
            case BINANCE_TEST -> {
                BinanceLimitOrder binanceLimitOrder = BinanceTransactions.buyTestCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty, price);
                if (binanceLimitOrder == null) return;

                LOG.trace("Placing Binance TEST BUY LIMIT order for {} of {} at {}", qty, base.getSymbol(), price);
                processTestTransaction(qty, binanceLimitOrder);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    public static boolean placeCurrencySellOrder(Broker broker, Currency base, Currency counter, BigDecimal price) {
        if (base.getQuantity().compareTo(BigDecimal.ZERO) == 0) return false;

        switch (broker) {
            case COINBASE -> LOG.info("Place COINBASE sell order here");
            case CEXIO -> LOG.info("Place CEXIO sell order here");
            case BINANCE -> LOG.info("Place BINANCE sell order here");
            case BINANCE_TEST -> {
                BinanceLimitOrder binanceLimitOrder = BinanceTransactions.sellTestCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), base.getQuantity(), price);
                if (binanceLimitOrder == null) return false;

                LOG.trace("Placing Binance TEST SELL LIMIT Order for {} of {} at {}", base.getQuantity(), base.getSymbol(), price);
                processTestTransaction(base.getQuantity(), binanceLimitOrder);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }

    private static void processTestTransaction(BigDecimal qty, BinanceLimitOrder binanceLimitOrder) {
        if (!Config.INSTANCE.isBackTest()) {
            Database.persistBinanceLimitOrder(binanceLimitOrder);
            BinanceLive.INSTANCE.getOrderHistoryLookup().setOrderId(binanceLimitOrder.getOrderId());
        }
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);

        // This code would normally be handled by the Order websocket feed
        binanceLimitOrder.setStatus(Order.OrderStatus.FILLED);
        binanceLimitOrder.setAveragePrice(BigDecimal.ONE);
        binanceLimitOrder.setCumulativeAmount(qty);
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
    }

    public static void placeSecurityBuyOrder(Broker broker, Security security, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + security.getSymbol() + " at " + price);

        switch (broker) {
            case ALPACA -> AlpacaTransactions.placeSecurityBuyOrder(security, qty, price);
            case ALPACA_TEST -> {
                AlpacaOrder alpacaOrder = AlpacaTransactions.placeTestSecurityBuyOrder(security, qty, price);

                // This code would normally be handled by the Order websocket feed
                if (alpacaOrder == null) return;
                alpacaOrder.setStatus(Order.OrderStatus.FILLED.toString());
                alpacaOrder.setFilledAt(ZonedDateTime.now());
                alpacaOrder.setFilledQty(qty.toPlainString());
                alpacaOrder.setFilledAvgPrice(price.toPlainString());
                AlpacaTransactions.processFilledOrder(alpacaOrder);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    public static boolean placeSecuritySellOrder(Broker broker, Security security, BigDecimal price, boolean sellAll) {
        BigDecimal[] qty = {BigDecimal.ZERO};

        security.getSecurityPositions().forEach(position -> {
            if (position.getPrice().compareTo(price) < 0 || sellAll) {
                LOG.trace("Create sell order for " + position.getQuantity() + " " + security.getSymbol() + " at " + price);
                qty[0] = qty[0].add(position.getQuantity());
            }
        });

        BigDecimal sellQty = qty[0];
        if (sellQty.equals(BigDecimal.ZERO)) return false;

        switch (broker) {
            case ALPACA -> AlpacaTransactions.placeSecuritySellOrder(security, sellQty, price);
            case ALPACA_TEST -> {
                AlpacaOrder alpacaOrder = AlpacaTransactions.placeTestSecuritySellOrder(security, sellQty, price);

                // This code would normally be handled by the Order websocket feed
                if (alpacaOrder == null) return false;
                alpacaOrder.setStatus(Order.OrderStatus.FILLED.toString());
                alpacaOrder.setFilledAt(ZonedDateTime.now());
                alpacaOrder.setFilledQty(sellQty.toPlainString());
                alpacaOrder.setFilledAvgPrice(price.toPlainString());
                AlpacaTransactions.processFilledOrder(alpacaOrder);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }

    public static boolean placeSecuritySellOrder(Broker broker, Security security, BigDecimal price) {
        return placeSecuritySellOrder(broker, security, price, false);
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
