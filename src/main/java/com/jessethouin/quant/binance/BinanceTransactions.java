package com.jessethouin.quant.binance;

import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_ACCOUNT_SERVICE;
import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_MIN_TRADES;
import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_TRADE_SERVICE;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Util;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;

@Component
public class BinanceTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTransactions.class);
    private static BinanceLive binanceLive;

    public BinanceTransactions(BinanceLive binanceLive) {
        BinanceTransactions.binanceLive = binanceLive;
    }

    public static void buyCurrency(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(currencyPair, qty, price, BID);
    }

    public static void sellCurrency(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(currencyPair, qty, price, ASK);
    }

    private static void transact(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (qty.multiply(price).compareTo(BINANCE_MIN_TRADES.get(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", BINANCE_MIN_TRADES.get(currencyPair), currencyPair.toString(), qty.multiply(price));
            return;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty.setScale(6, RoundingMode.FLOOR))
                .limitPrice(price.setScale(8, RoundingMode.FLOOR))
//                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        try {
            LOG.info("Limit Order: " + limitOrder.toString());
            String id = BINANCE_TRADE_SERVICE.placeLimitOrder(limitOrder);
            binanceLive.getOrderHistoryLookup().setOrderId(Long.parseLong(id));
        } catch (Exception e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void buyTestCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        testTransact(portfolio, currencyPair, qty, price, BID);
    }

    public static void sellTestCurrency(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        testTransact(portfolio, currencyPair, qty, price, ASK);
    }

    private static void testTransact(Portfolio portfolio, CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (!CONFIG.isBackTest() && qty.multiply(price).compareTo(BINANCE_MIN_TRADES.get(currencyPair)) < 0) {
            LOG.warn("Trade must be minimum of {} for {}. Was {}.", BINANCE_MIN_TRADES.get(currencyPair), currencyPair.toString(), qty.multiply(price));
            return;
        }

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .id(String.valueOf(Math.random() * Integer.MIN_VALUE))
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty)
                .limitPrice(price)
                .fee(price.multiply(qty).multiply(CONFIG.getFee()).setScale(8, RoundingMode.HALF_UP))
                .flag(TimeInForce.GTC)
                .timestamp(new Date())
                .build();
        LOG.trace("Test Limit Order: " + limitOrder.toString());
        processTestTransaction(limitOrder, portfolio);
    }

    private static void processTestTransaction(LimitOrder limitOrder, Portfolio portfolio) {
        // Mocking remote NEW order
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
        LOG.trace("New Test Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        if (!CONFIG.isBackTest()) {
            binanceLive.getOrderHistoryLookup().setOrderId(Long.parseLong(binanceLimitOrder.getId()));
        }
        processBinanceLimitOrder(binanceLimitOrder);

        // Mocking remote FILLED order
        binanceLimitOrder.setStatus(Order.OrderStatus.FILLED);
        binanceLimitOrder.setAveragePrice(BigDecimal.ONE);
        binanceLimitOrder.setCumulativeAmount(limitOrder.getOriginalAmount());
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
                    case NEW -> Util.debit(counter, originalAmount.multiply(limitPrice), "New Binance Buy Limit order");
                    case FILLED, PARTIALLY_FILLED -> {
                        Util.credit(base, originalAmount, "Filled or Partially Filled Binance Buy Limit order");
                        Util.debit(counter, fee, "Fee for Filled or Partially Filled Binance Buy Limit order");
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(counter, originalAmount.multiply(limitPrice), "Cancelled Binance Buy Limit order");
                }
            }
            case ASK -> {
                switch (binanceLimitOrder.getStatus()) {
                    case NEW -> Util.debit(base, originalAmount, "New Binance Sell Limit order");
                    case FILLED, PARTIALLY_FILLED -> {
                        Util.credit(counter, binanceLimitOrder.getCumulativeAmount().multiply(limitPrice), "Filled or Partially Filled Binance Sell Limit order");
                        Util.debit(counter, fee, "Fee for Filled or Partially Filled Binance Sell Limit order");
                    }
                    case CANCELED, EXPIRED, REJECTED, REPLACED -> Util.credit(base, originalAmount, "Cancelled Binance Sell Limit order");
                }
            }
            default -> LOG.warn("binanceLimitOrder type unknown: {}", binanceLimitOrder.getType());
        }
    }

    public static void showWallets() {
        try {
            StringBuilder sb = new StringBuilder();
            BINANCE_ACCOUNT_SERVICE.getAccountInfo().getWallets().forEach((s, w) -> {
                w.getBalances().forEach((c, b) -> {
                    if (b.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                        sb.append(System.lineSeparator());
                        sb.append("\t");
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

    public static void showTradingFees(Portfolio portfolio) {
        try {
            Map<CurrencyPair, Fee> dynamicTradingFees = BINANCE_ACCOUNT_SERVICE.getDynamicTradingFees();
            dynamicTradingFees.forEach((c, f) -> {
                if (portfolio.getCurrencies().stream().anyMatch(currency -> c.base.getSymbol().equals(currency.getSymbol())) &&
                        portfolio.getCurrencies().stream().anyMatch(currency -> c.counter.getSymbol().equals(currency.getSymbol()))) {
                    LOG.info(c.toString() + " - m : " + f.getMakerFee() + " t : " + f.getTakerFee());
                }
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}