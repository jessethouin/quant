package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.db.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.trade.TimeInForce;
import org.knowm.xchange.binance.service.BinanceAccountService;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

import static com.jessethouin.quant.binance.BinanceLive.INSTANCE;
import static org.knowm.xchange.binance.dto.trade.OrderType.LIMIT;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

public class BinanceTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTransactions.class);

    public static void buyCurrency(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(currencyPair, qty, price, BID);
    }

    public static void sellCurrency(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price) {
        transact(currencyPair, qty, price, ASK);
    }

    private static void transact(CurrencyPair currencyPair, BigDecimal qty, BigDecimal price, OrderType orderType) {
        if (qty.equals(BigDecimal.ZERO)) return;

        LimitOrder limitOrder = new LimitOrder.Builder(orderType, currencyPair)
                .originalAmount(qty)
                .limitPrice(price)
                .flag(TimeInForce.GTC)
                .build();
        try {
            ((BinanceTradeService) INSTANCE.getBinanceExchange().getTradeService()).placeTestOrder(LIMIT, limitOrder, limitOrder.getLimitPrice(), null);
            BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder);
            Database.persistBinanceLimitOrder(binanceLimitOrder);
            LOG.info("Limit Order: " + limitOrder.toString());
            LOG.info("Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void processOrders(BinanceLimitOrder binanceLimitOrder) {

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
            dynamicTradingFees.forEach((c, f) -> {
                LOG.info(c.toString() + " - m : " + f.getMakerFee() + " t : " + f.getTakerFee());
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}