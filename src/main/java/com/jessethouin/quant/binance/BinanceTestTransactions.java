package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.broker.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_MIN_TRADES;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

@Service
public class BinanceTestTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTestTransactions.class);
    private static WebClient binancePublishWebClient;

    public BinanceTestTransactions(WebClient binancePublishWebClient) {
        BinanceTestTransactions.binancePublishWebClient = binancePublishWebClient;
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
                .orderStatus(Order.OrderStatus.NEW)
                .originalAmount(qty)
                .limitPrice(price)
                .fee(BigDecimal.ZERO)
                .timestamp(new Date())
                .build();

        if (CONFIG.isBackTest()) {
            final LimitOrder limitOrderWithId = LimitOrder.Builder.from(limitOrder).id(String.valueOf(new Date().getTime())).build();
            processTestTransaction(limitOrderWithId, portfolio);
        } else {
            try {
                LOG.info("Our Test Limit Order build (local): {}", limitOrder.toString());
                String id = binancePublishWebClient.post().bodyValue(limitOrder).retrieve().bodyToMono(String.class).block();
                LOG.info("Order id received from remote server: {}", id);
                if (id == null) throw new Exception("Limit Order id was null from server.");
                BinanceStreamProcessor.getOrderHistoryLookup().setOrderId(id);
            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }
    }

    private static void processTestTransaction(LimitOrder limitOrder, Portfolio portfolio) {
        // Mocking remote NEW order
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        portfolio.getBinanceLimitOrders().add(binanceLimitOrder);
        binanceLimitOrder.setCommissionAsset(null);
        binanceLimitOrder.setCommissionAmount(null);
        LOG.debug("New Test Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        BinanceTransactions.updateBinanceLimitOrder(binanceLimitOrder, limitOrder);
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);

        // Mocking remote FILLED order
        limitOrder.setOrderStatus(Order.OrderStatus.FILLED);
        limitOrder.setAveragePrice(BigDecimal.ONE);
        limitOrder.setCumulativeAmount(limitOrder.getOriginalAmount());
        Currency commissionAsset = Util.getCurrencyFromPortfolio(((CurrencyPair) limitOrder.getInstrument()).base.getSymbol(), portfolio);
        BigDecimal commissionAmount = limitOrder.getOriginalAmount().multiply(CONFIG.getFee()).setScale(8, RoundingMode.HALF_UP);

        binanceLimitOrder.setCommissionAsset(commissionAsset);
        binanceLimitOrder.setCommissionAmount(commissionAmount);
        LOG.debug("Updated Test Binance Limit order: " + binanceLimitOrder.toString().replace(",", ",\n\t"));
        BinanceTransactions.updateBinanceLimitOrder(binanceLimitOrder, limitOrder);
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
    }
}
