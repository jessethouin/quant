package com.jessethouin.quant.binance;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.binance.BinanceLive.Ref;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.OrderHistoryLookup;
import com.jessethouin.quant.binance.beans.repos.BinanceLimitOrderRepository;
import com.jessethouin.quant.broker.Util;
import info.bitrich.xchangestream.binance.dto.ExecutionReportBinanceUserTransaction;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class BinanceStreamProcessing {
    private static final Logger LOG = LogManager.getLogger(BinanceStreamProcessing.class);

    @Getter
    static OrderHistoryLookup orderHistoryLookup;
    static BinanceLive binanceLive;
    static BinanceLimitOrderRepository binanceLimitOrderRepository;

    public BinanceStreamProcessing(BinanceLive binanceLive, BinanceLimitOrderRepository binanceLimitOrderRepository) {
        BinanceStreamProcessing.binanceLive = binanceLive;
        BinanceStreamProcessing.binanceLimitOrderRepository = binanceLimitOrderRepository;
    }

    public static void processMarketData(Ref ref) {
        ref.shortMAValue = Util.getMA(ref.previousShortMAValue, CONFIG.getShortLookback(), ref.price);
        ref.longMAValue = Util.getMA(ref.previousLongMAValue, CONFIG.getLongLookback(), ref.price);

        orderHistoryLookup = new OrderHistoryLookup();

        ref.c.updateCalc(ref.price, ref.shortMAValue, ref.longMAValue);
        ref.c.decide();

        BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder().timestamp(requireNonNullElse(ref.timestamp, new Date())).ma1(ref.shortMAValue).ma2(ref.longMAValue).l(ref.c.getLow()).h(ref.c.getHigh()).p(ref.price).build();
        BigDecimal value = Util.getValueAtPrice(ref.baseCurrency, ref.price).add(ref.counterCurrency.getQuantity());

        orderHistoryLookup.setTradeId(binanceTradeHistory.getTradeId());
        orderHistoryLookup.setValue(value);
        BinanceUtil.reconcile(binanceLive.getPortfolio());

        LOG.info("{}/{} - {} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}", ref.baseCurrency.getSymbol(), ref.counterCurrency.getSymbol(), ref.count, ref.shortMAValue, ref.longMAValue, ref.c.getLow(), ref.c.getHigh(), ref.price, value);

        ref.previousShortMAValue = ref.shortMAValue;
        ref.previousLongMAValue = ref.longMAValue;
        ref.count++;
    }

    public static synchronized void processRemoteOrder(ExecutionReportBinanceUserTransaction er) {
        LOG.info("Execution Report: {}", er);
        Order order = er.toOrder();
        if (er.getCommissionAsset() == null) {
            processRemoteOrder(order);
        } else {
            processRemoteOrder(order, Util.getCurrencyFromPortfolio(er.getCommissionAsset(), binanceLive.getPortfolio()), er.getCommissionAmount());
        }
    }

    public static synchronized void processRemoteOrder(Order order) {
        processRemoteOrder(order, null, null);
    }

    public static synchronized void processRemoteOrder(Order order, Currency commissionAsset, BigDecimal commissionAmount) {
        LOG.info("Remote Order: {}", order);
        if (order instanceof LimitOrder) {
            LimitOrder limitOrder = (LimitOrder) order;

            BinanceLimitOrder binanceLimitOrder = binanceLive.getPortfolio().getBinanceLimitOrders().stream().filter(blo -> blo.getId().equals(limitOrder.getId())).findFirst().orElse(null);

            if (binanceLimitOrder == null) {
                binanceLimitOrder = binanceLimitOrderRepository.getById(limitOrder.getId());
            }

            if (binanceLimitOrder == null) {
                binanceLimitOrder = new BinanceLimitOrder(limitOrder, binanceLive.getPortfolio());
                binanceLive.getPortfolio().getBinanceLimitOrders().add(binanceLimitOrder);
            }

            binanceLimitOrder.setCommissionAsset(commissionAsset);
            binanceLimitOrder.setCommissionAmount(commissionAmount);
            BinanceTransactions.updateBinanceLimitOrder(binanceLimitOrder, limitOrder);
            BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
        }
    }
}
