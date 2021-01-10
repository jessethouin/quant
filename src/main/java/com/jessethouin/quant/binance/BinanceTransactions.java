package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.CurrencyPosition;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static com.jessethouin.quant.binance.BinanceLive.INSTANCE;
import static org.knowm.xchange.binance.dto.trade.OrderType.LIMIT;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

public class BinanceTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTransactions.class);

    public static void buyCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(portfolio, currencyPair, qty, price, BID);
    }

    public static void sellCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(portfolio, currencyPair, qty, price, ASK);
    }

    private static void transact(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (qty.equals(BigDecimal.ZERO)) return;

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty)
                .limitPrice(price)
                .fee(price.multiply(qty).multiply(BigDecimal.valueOf(.00075)))
                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        try {
            ((BinanceTradeService) INSTANCE.getBinanceExchange().getTradeService()).placeTestOrder(LIMIT, limitOrder, limitOrder.getLimitPrice(), null);
            BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
            BinanceLive.INSTANCE.getOrderHistoryLookup().setOrderId(binanceLimitOrder.getOrderId());
            Database.persistBinanceLimitOrder(binanceLimitOrder);
            LOG.debug("Limit Order: " + limitOrder.toString());
            LOG.debug("Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static BinanceLimitOrder buyTestCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        return testTransact(portfolio, currencyPair, qty, price, BID);
    }

    public static BinanceLimitOrder sellTestCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        return testTransact(portfolio, currencyPair, qty, price, ASK);
    }

    private static BinanceLimitOrder testTransact(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (!Config.INSTANCE.getBackTest() && qty.multiply(price).compareTo(Util.getMinTrade(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", Util.getMinTrade(currencyPair), currencyPair.toString(), qty.multiply(price));
            return null;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty)
                .limitPrice(price)
                .fee(price.multiply(qty).multiply(BigDecimal.valueOf(.00075)))
                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
        LOG.trace("Limit Order: " + limitOrder.toString());
        LOG.trace("Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        return binanceLimitOrder;
    }

    public static void processBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder) {
        processBinanceLimitOrder(binanceLimitOrder, Collections.emptyList());
    }

    public static void processBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder, List<CurrencyPosition> eligibleCurrencyPositions) {
        Portfolio portfolio = binanceLimitOrder.getPortfolio();
        CurrencyPair currencyPair = new CurrencyPair(binanceLimitOrder.getInstrument());
        Currency base = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        Currency counter = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
        BigDecimal limitPrice = binanceLimitOrder.getLimitPrice();
        BigDecimal originalAmount = binanceLimitOrder.getOriginalAmount();
        BigDecimal fee = binanceLimitOrder.getFee();

        switch (binanceLimitOrder.getType()) {
            case BID -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> reduceCurrency(counter, originalAmount.multiply(limitPrice), eligibleCurrencyPositions); //todo subtract fees as well
                    case FILLED -> {
                        Transactions.addCurrencyPosition(portfolio, originalAmount, base, counter, limitPrice);
                        reduceCurrency(counter, fee, new ArrayList<>(counter.getCurrencyPositions()));
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Transactions.addCurrencyPosition(portfolio, originalAmount.multiply(limitPrice), counter, base, limitPrice);
                }
            }
            case ASK -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> reduceCurrency(base, originalAmount, eligibleCurrencyPositions); //todo subtract fees as well
                    case FILLED -> {
                        BigDecimal filledQty = binanceLimitOrder.getCumulativeAmount();
                        BigDecimal filledAvgPrice = limitPrice.multiply(binanceLimitOrder.getAveragePrice());
                        Transactions.addCurrencyPosition(portfolio, filledQty.multiply(filledAvgPrice), counter, base, filledAvgPrice);
                        reduceCurrency(counter, fee, new ArrayList<>(counter.getCurrencyPositions()));
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Transactions.addCurrencyPosition(portfolio, originalAmount, base, counter, limitPrice);
                }
            }
            default -> LOG.warn("binanceLimitOrder type unknown: {}", binanceLimitOrder.getType());
        }
    }

    private static void reduceCurrency(Currency currency, BigDecimal removeQty, List<CurrencyPosition> remove) {
        BigDecimal[] qty = {removeQty};
        remove.sort(Comparator.comparing(CurrencyPosition::getPrice).reversed());

        remove.forEach(currencyPosition -> {
            BigDecimal currencyPositionQuantity = currencyPosition.getQuantity();
            if (currencyPositionQuantity.compareTo(qty[0]) <= 0) {
                currency.getCurrencyPositions().remove(currencyPosition);
                qty[0] = qty[0].subtract(currencyPositionQuantity);
            } else {
                currencyPosition.setQuantity(currencyPositionQuantity.subtract(qty[0]));
                qty[0] = BigDecimal.ZERO;
            }
        });
    }

    public void showWallets() {
        BinanceAccountService accountService = (BinanceAccountService) INSTANCE.getBinanceExchange().getAccountService();

        try {
            StringBuilder sb = new StringBuilder();
            accountService.getAccountInfo().getWallets().forEach((s, w) -> {
                sb.append(System.lineSeparator());
                w.getBalances().forEach((c, b) -> {
                    if (b.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                        sb.append(System.lineSeparator());
                        sb.append(c.getSymbol());
                        sb.append(" - ");
                        sb.append(b.getTotal().toPlainString());
                    }
                });
                LOG.info("Wallet: {}", sb);
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public void showTradingFees() {
        BinanceAccountService accountService = (BinanceAccountService) INSTANCE.getBinanceExchange().getAccountService();

        try {
            Map<CurrencyPair, Fee> dynamicTradingFees = accountService.getDynamicTradingFees();
            dynamicTradingFees.forEach((c, f) -> LOG.info(c.toString() + " - m : " + f.getMakerFee() + " t : " + f.getTakerFee()));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}