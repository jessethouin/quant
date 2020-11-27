package com.jessethouin.quant.broker;

import com.jessethouin.quant.alpaca.AlpacaTransactions;
import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.beans.*;
import com.jessethouin.quant.binance.BinanceTransactions;
import com.jessethouin.quant.conf.Broker;
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
        if (qty.equals(BigDecimal.ZERO)) return;

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

    public static void placeCurrencyBuyOrder(Broker broker, Currency base, Currency counter, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + base.getSymbol() + " at " + price);

        switch (broker) {
            case COINBASE -> {
                LOG.info("Place COINBASE buy order here");
            }
            case CEXIO -> {
                LOG.info("Place CEXIO buy order here");
            }
            case BINANCE -> {
                LOG.info("Place Binance buy order here");
                BinanceTransactions.buyCurrency(new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty, price);
            }
            default -> throw new IllegalStateException("Unexpected value: " + broker);
        }
    }

    public static boolean placeCurrencySellOrder(Broker broker, Currency base, Currency counter, BigDecimal price, boolean sellAll) {
        List<CurrencyPosition> remove = new ArrayList<>();
        BigDecimal[] qty = {BigDecimal.ZERO};

        base.getCurrencyPositions().stream().filter(currencyPosition -> currencyPosition.getCounterCurrency() == null || currencyPosition.getCounterCurrency().equals(counter)).forEach(position -> {
            if (position.getPrice().compareTo(price) < 0 || sellAll) {
                LOG.trace("Create sell order for " + position.getQuantity() + " " + base.getSymbol() + " at " + price);
                remove.add(position);
                qty[0] = qty[0].add(position.getQuantity());
            }
        });

        if (!remove.isEmpty()) {
            switch (broker) {
                case COINBASE -> {
                    LOG.info("Place COINBASE sell order here");
                }
                case CEXIO -> {
                    LOG.info("Place CEXIO sell order here");
                }
                case BINANCE -> {
                    LOG.info("Place BINANCE sell order here");
                    BinanceTransactions.sellCurrency(new CurrencyPair(base.getSymbol(), counter.getSymbol()), qty[0], price);
                }
                default -> throw new IllegalStateException("Unexpected value: " + broker);
            }
//            Add logic to store positions for removal based on success of sell order placed
//            base.getCurrencyPositions().removeAll(remove);
            return true;
        }

        return false;
    }

    public static void placeSecurityBuyOrder(Broker broker, Security security, BigDecimal qty, BigDecimal price) {
        LOG.trace("Create buy order for " + qty + " " + security.getSymbol() + " at " + price);

        switch (broker) {
            case ALPACA -> {
                LOG.info("Placing ALPACA order here");
                AlpacaTransactions.placeSecurityBuyOrder(security, qty, price);
            }
            case ALPACA_TEST -> {
                LOG.info("Placing ALPACA_TEST order here");
                AlpacaOrder alpacaOrder = AlpacaTransactions.placePaperSecurityBuyOrder(security, qty, price);

                LOG.info("Processing ALPACA_TEST order here");
                if (alpacaOrder == null) return;
                alpacaOrder.setStatus(Order.OrderStatus.FILLED.toString());
                alpacaOrder.setFilledAt(ZonedDateTime.now());
                alpacaOrder.setFilledQty(qty.toPlainString());
                alpacaOrder.setFilledAvgPrice(price.toPlainString());
                AlpacaTransactions.processFilledOrder(alpacaOrder);
            }
            default -> throw new IllegalStateException("Unexpected value: " + broker);
        }
    }

    public static boolean placeSecuritySellOrder(Broker broker, Security security, BigDecimal price, boolean sellAll) {
        List<SecurityPosition> remove = new ArrayList<>();
        BigDecimal[] qty = {BigDecimal.ZERO};

        security.getSecurityPositions().forEach(position -> {
            if (position.getPrice().compareTo(price) < 0 || sellAll) {
                LOG.trace("Create sell order for " + position.getQuantity() + " " + security.getSymbol() + " at " + price);
                remove.add(position);
                qty[0] = qty[0].add(position.getQuantity());
            }
        });

        if (!remove.isEmpty()) {
            switch (broker) {
                case ALPACA -> {
                    LOG.info("Place ALPACA sell order here");
                    AlpacaTransactions.placeSecuritySellOrder(security, qty[0], price);
                }
                case ALPACA_TEST -> {
                    LOG.info("Place ALPACA_TEST sell order here");
                    AlpacaOrder alpacaOrder = AlpacaTransactions.placePaperSecuritySellOrder(security, qty[0], price);

                    if (alpacaOrder == null) return false;
                    alpacaOrder.setStatus(Order.OrderStatus.FILLED.toString());
                    alpacaOrder.setFilledAt(ZonedDateTime.now());
                    alpacaOrder.setFilledQty(qty[0].toPlainString());
                    alpacaOrder.setFilledAvgPrice(price.toPlainString());
                    AlpacaTransactions.processFilledOrder(alpacaOrder);
                }
                default -> throw new IllegalStateException("Unexpected value: " + broker);
            }
//            Add logic to store positions for removal based on success of sell order placed
//            security.getSecurityPositions().removeAll(remove);
            return true;
        }

        return false;
    }

    public static boolean placeSecuritySellAllOrder(Broker broker, Security security, BigDecimal price) {
        return placeSecuritySellOrder(broker, security, price, true);
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

        if (counter != null) {
            CurrencyPosition debit = new CurrencyPosition();
            debit.setOpened(opened);
            debit.setQuantity(qty.negate());
            debit.setPrice(price);
            debit.setBaseCurrency(counter);
            debit.setCounterCurrency(base);
            counter.getCurrencyPositions().add(debit);
        }

        Database.persistPortfolio(portfolio);
    }

    public static void addSecurityPosition(Security security, BigDecimal qty, BigDecimal price) {
        if (qty.compareTo(BigDecimal.ZERO) == 0) return;

        SecurityPosition securityPosition = new SecurityPosition();
        securityPosition.setQuantity(qty);
        securityPosition.setPrice(price);
        securityPosition.setSecurity(security);
        securityPosition.setOpened(new Date());
        security.getSecurityPositions().add(securityPosition);

        Database.persistSecurity(security);
    }
}
