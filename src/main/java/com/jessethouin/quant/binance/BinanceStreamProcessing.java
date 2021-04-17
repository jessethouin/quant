package com.jessethouin.quant.binance;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.binance.beans.repos.OrderHistoryLookupRepository;
import com.jessethouin.quant.broker.Fundamentals;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import com.jessethouin.quant.binance.beans.OrderHistoryLookup;
import com.jessethouin.quant.binance.beans.repos.BinanceLimitOrderRepository;
import com.jessethouin.quant.binance.beans.repos.BinanceTradeHistoryRepository;
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
    static BinanceTradeHistoryRepository binanceTradeHistoryRepository;
    static OrderHistoryLookupRepository orderHistoryLookupRepository;

    public BinanceStreamProcessing(BinanceLive binanceLive, BinanceLimitOrderRepository binanceLimitOrderRepository, BinanceTradeHistoryRepository binanceTradeHistoryRepository, OrderHistoryLookupRepository orderHistoryLookupRepository) {
        BinanceStreamProcessing.binanceLive = binanceLive;
        BinanceStreamProcessing.binanceLimitOrderRepository = binanceLimitOrderRepository;
        BinanceStreamProcessing.binanceTradeHistoryRepository = binanceTradeHistoryRepository;
        BinanceStreamProcessing.orderHistoryLookupRepository = orderHistoryLookupRepository;
    }

    public static void processMarketData(Fundamentals fundamentals) {
        fundamentals.setShortMAValue(Util.getMA(fundamentals.getPreviousShortMAValue(), CONFIG.getShortLookback(), fundamentals.getPrice()));
        fundamentals.setLongMAValue(Util.getMA(fundamentals.getPreviousLongMAValue(), CONFIG.getLongLookback(), fundamentals.getPrice()));

        orderHistoryLookup = new OrderHistoryLookup();

        fundamentals.getCalc().updateCalc(fundamentals.getPrice(), fundamentals.getShortMAValue(), fundamentals.getLongMAValue());
        fundamentals.getCalc().decide();

        BinanceTradeHistory binanceTradeHistory = BinanceTradeHistory.builder()
            .timestamp(requireNonNullElse(fundamentals.getTimestamp(), new Date()))
            .ma1(fundamentals.getShortMAValue())
            .ma2(fundamentals.getLongMAValue())
            .l(fundamentals.getCalc().getLow())
            .h(fundamentals.getCalc().getHigh())
            .p(fundamentals.getPrice())
            .build();
        BigDecimal value = Util.getValueAtPrice(fundamentals.getBaseCurrency(), fundamentals.getPrice()).add(fundamentals.getCounterCurrency().getQuantity());

        orderHistoryLookup.setTradeId(binanceTradeHistoryRepository.save(binanceTradeHistory).getTradeId());
        orderHistoryLookup.setValue(value);
        orderHistoryLookupRepository.save(orderHistoryLookup);

        LOG.info("{}/{} - {} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}",
            fundamentals.getBaseCurrency().getSymbol(),
            fundamentals.getCounterCurrency().getSymbol(),
            fundamentals.count,
            fundamentals.getShortMAValue(),
            fundamentals.getLongMAValue(),
            fundamentals.getCalc().getLow(),
            fundamentals.getCalc().getHigh(),
            fundamentals.getPrice(),
            value);

        fundamentals.setPreviousShortMAValue(fundamentals.getShortMAValue());
        fundamentals.setPreviousLongMAValue(fundamentals.getLongMAValue());
        fundamentals.count++;
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
