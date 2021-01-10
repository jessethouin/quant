package com.jessethouin.quant.broker;

import com.jessethouin.quant.alpaca.AlpacaTransactions;
import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.*;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
            return placeCurrencySellOrder(broker, base, counter, price, false);
        }
    }

    public static void placeSellAllOrder(Broker broker, Security security, Currency base, Currency counter, BigDecimal price) {
        if (security != null) {
            placeSecuritySellOrder(broker, security, price);
        } else {
            placeCurrencySellOrder(broker, base, counter, price, true);
        }
    }

    public static void placeCurrencyBuyOrder(Broker broker, Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + base.getSymbol() + " at " + price);

        switch (broker) {
            case COINBASE -> LOG.info("Place COINBASE buy order here");
            case CEXIO -> LOG.info("Place CEXIO buy order here");
            case BINANCE -> {
                LOG.info("Placing Binance BUY order for {} of {} at {}", qty, base.getSymbol(), price);
                BinanceTransactions.buyCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty, price);
            }
            case BINANCE_TEST -> {
                BinanceLimitOrder binanceLimitOrder = BinanceTransactions.buyTestCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty, price);
                if (binanceLimitOrder == null) return;

                LOG.trace("Placing Binance TEST BUY LIMIT order for {} of {} at {}", qty, base.getSymbol(), price);
                processTestTransaction(qty, new ArrayList<>(counter.getCurrencyPositions()), binanceLimitOrder);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
    }

    public static boolean placeCurrencySellOrder(Broker broker, Currency base, Currency counter, BigDecimal price, boolean sellAll) {
        List<CurrencyPosition> currencyPositions = getSellableCurrencyPositions(base, counter, price, sellAll);
        BigDecimal sellQty = currencyPositions.stream().map(CurrencyPosition::getQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sellQty.equals(BigDecimal.ZERO)) return false;

        switch (broker) {
            case COINBASE -> LOG.info("Place COINBASE sell order here");
            case CEXIO -> LOG.info("Place CEXIO sell order here");
            case BINANCE -> {
                LOG.info("Place BINANCE sell order here");
                BinanceTransactions.sellCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), sellQty, price);
            }
            case BINANCE_TEST -> {
                BinanceLimitOrder binanceLimitOrder = BinanceTransactions.sellTestCurrency(base.getPortfolio(), new CurrencyPair(base.getSymbol(), counter.getSymbol()), sellQty, price);
                if (binanceLimitOrder == null) return false;

                LOG.trace("Placing Binance TEST SELL LIMIT Order for {} of {} at {}", sellQty, base.getSymbol(), price);
                processTestTransaction(sellQty, currencyPositions, binanceLimitOrder);
            }
            default -> throw new IllegalStateException("Unexpected broker: " + broker);
        }
        return true;
    }

    public static List<CurrencyPosition> getSellableCurrencyPositions(CurrencyPair currencyPair, Portfolio portfolio, BigDecimal price, boolean sellAll) {
        return getSellableCurrencyPositions(Util.getCurrency(portfolio, currencyPair.base.getSymbol()), Util.getCurrency(portfolio, currencyPair.counter.getSymbol()), price, sellAll);
    }

    public static List<CurrencyPosition> getSellableCurrencyPositions(Currency base, Currency counter, BigDecimal price, boolean sellAll) {
        List<CurrencyPosition> currencyPositions = new ArrayList<>();

        base.getCurrencyPositions().forEach(position -> {
            BigDecimal positionValue = Util.getCurrencyPositionValue(position, counter);
            LOG.trace("positionValue: {}, loss: {}, gain: {}, price: {}", positionValue, positionValue.multiply(Config.INSTANCE.getLoss()), positionValue.multiply(Config.INSTANCE.getGain()), price);
            if (price.compareTo(positionValue.multiply(Config.INSTANCE.getGain())) > 0 || price.compareTo(positionValue.multiply(Config.INSTANCE.getLoss())) < 0 || sellAll) {
                LOG.trace("Create sell order for " + position.getQuantity() + " " + base.getSymbol() + " at " + price);
                currencyPositions.add(position);
            }
        });
        return currencyPositions;
    }

    private static void processTestTransaction(BigDecimal qty, List<CurrencyPosition> currencyPositions, BinanceLimitOrder binanceLimitOrder) {
        if (!Config.INSTANCE.getBackTest()) {
            Database.persistBinanceLimitOrder(binanceLimitOrder);
            BinanceLive.INSTANCE.getOrderHistoryLookup().setOrderId(binanceLimitOrder.getOrderId());
        }
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder, currencyPositions);

        // This code would normally be handled by the Order websocket feed
        binanceLimitOrder.setStatus(Order.OrderStatus.FILLED);
        binanceLimitOrder.setAveragePrice(BigDecimal.ONE);
        binanceLimitOrder.setCumulativeAmount(qty);
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder, currencyPositions);
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

    public static void addCurrencyPosition(Portfolio portfolio, BigDecimal qty, Currency base) {
        addCurrencyPosition(portfolio, qty, base, null, null);
    }

    public static void addCurrencyPosition(Portfolio portfolio, BigDecimal qty, Currency base, Currency counter, BigDecimal price) {
        if (qty.compareTo(BigDecimal.ZERO) == 0) return;

        if (qty.compareTo(BigDecimal.ZERO) < 0 && qty.abs().compareTo(Util.getBalance(portfolio, base)) > 0) {
            LOG.error(String.format("You don't have enough cash to deduct %s. Your balance is %s.", qty, Util.getBalance(portfolio, base)));
            return;
        }

        Date opened = new Date();

        CurrencyPosition credit = new CurrencyPosition();
        credit.setOpened(opened);
        credit.setQuantity(qty);
        credit.setPrice(price);
        credit.setBaseCurrency(base);
        credit.setCounterCurrency(counter);
        base.getCurrencyPositions().add(credit);

//        Database.persistPortfolio(portfolio);
    }

    public static void addSecurityPosition(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.compareTo(BigDecimal.ZERO) == 0) return;

        SecurityPosition securityPosition = new SecurityPosition();
        securityPosition.setQuantity(qty);
        securityPosition.setPrice(price);
        securityPosition.setSecurity(security);
        securityPosition.setOpened(new Date());
        security.getSecurityPositions().add(securityPosition);

//        Database.persistSecurity(security);
    }
}
