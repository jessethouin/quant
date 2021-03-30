package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.repos.BinanceLimitOrderRepository;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;

import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

@Component
public class BinanceTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTransactions.class);
    private static BinanceLive binanceLive;
    private static BinanceLimitOrderRepository binanceLimitOrderRepository;

    public BinanceTransactions(BinanceLive binanceLive, BinanceLimitOrderRepository binanceLimitOrderRepository) {
        BinanceTransactions.binanceLive = binanceLive;
        BinanceTransactions.binanceLimitOrderRepository = binanceLimitOrderRepository;
    }

    public static void buyCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(portfolio, currencyPair, qty, price, BID);
    }

    public static void sellCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(portfolio, currencyPair, qty, price, ASK);
    }

    private static void transact(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (qty.multiply(price).compareTo(binanceLive.getMinTrades().get(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", binanceLive.getMinTrades().get(currencyPair), currencyPair.toString(), qty.multiply(price));
            return;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty.setScale(6, RoundingMode.FLOOR))
                .limitPrice(price.setScale(8, RoundingMode.FLOOR))
                .fee(price.multiply(qty).multiply(Config.INSTANCE.getFee()).setScale(8, RoundingMode.HALF_UP))
                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        try {
            LOG.debug("Limit Order: " + limitOrder.toString());
            binanceLive.getBinanceExchange().getTradeService().placeLimitOrder(limitOrder);
        } catch (Exception e) {
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
        if (!Config.INSTANCE.isBackTest() && qty.multiply(price).compareTo(binanceLive.getMinTrades().get(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", binanceLive.getMinTrades().get(currencyPair), currencyPair.toString(), qty.multiply(price));
            return null;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty)
                .limitPrice(price)
                .fee(price.multiply(qty).multiply(Config.INSTANCE.getFee()).setScale(8, RoundingMode.HALF_UP))
                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
        LOG.trace("Limit Order: " + limitOrder.toString());
        LOG.trace("Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        return binanceLimitOrder;
    }

    public static void processTestTransaction(BigDecimal qty, BinanceLimitOrder binanceLimitOrder) {
        if (!Config.INSTANCE.isBackTest()) {
            binanceLimitOrderRepository.save(binanceLimitOrder);
            binanceLive.getOrderHistoryLookup().setOrderId(binanceLimitOrder.getOrderId());
        }
        processBinanceLimitOrder(binanceLimitOrder);

        // This code would normally be handled by the Order websocket feed
        binanceLimitOrder.setStatus(Order.OrderStatus.FILLED);
        binanceLimitOrder.setAveragePrice(BigDecimal.ONE);
        binanceLimitOrder.setCumulativeAmount(qty);
        processBinanceLimitOrder(binanceLimitOrder);
    }

    public static void processBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder) {
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
                    case NEW -> Util.debit(counter, originalAmount.multiply(limitPrice));
                    case FILLED -> {
                        Util.credit(base, originalAmount);
                        Util.debit(counter, fee);
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(counter, originalAmount.multiply(limitPrice));
                }
            }
            case ASK -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> Util.debit(base, originalAmount);
                    case FILLED -> {
                        BigDecimal filledQty = binanceLimitOrder.getCumulativeAmount();
                        BigDecimal filledAvgPrice = limitPrice.multiply(binanceLimitOrder.getAveragePrice());
                        Util.credit(counter, filledQty.multiply(filledAvgPrice));
                        Util.debit(counter, fee);
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(base, originalAmount);
                }
            }
            default -> LOG.warn("binanceLimitOrder type unknown: {}", binanceLimitOrder.getType());
        }
    }

    public static void showWallets() {
        BinanceAccountService accountService = (BinanceAccountService) binanceLive.getBinanceExchange().getAccountService();

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

    public static void showTradingFees() {
        BinanceAccountService accountService = (BinanceAccountService) binanceLive.getBinanceExchange().getAccountService();

        try {
            Map<CurrencyPair, Fee> dynamicTradingFees = accountService.getDynamicTradingFees();
            dynamicTradingFees.forEach((c, f) -> LOG.info(c.toString() + " - m : " + f.getMakerFee() + " t : " + f.getTakerFee()));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}