package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.repos.OrderHistoryLookupRepository;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.repos.BinanceLimitOrderRepository;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.common.StreamProcessor;
import com.jessethouin.quant.conf.Broker;
import com.jessethouin.quant.conf.CurrencyType;
import info.bitrich.xchangestream.binance.dto.ExecutionReportBinanceUserTransaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.jessethouin.quant.conf.Config.CONFIG;

@Component
@Transactional
public class BinanceStreamProcessor extends StreamProcessor {
    private static final Logger LOG = LogManager.getLogger(BinanceStreamProcessor.class);

    static BinanceLive binanceLive;
    static BinanceLimitOrderRepository binanceLimitOrderRepository;

    public BinanceStreamProcessor(BinanceLive binanceLive, BinanceLimitOrderRepository binanceLimitOrderRepository, TradeHistoryRepository tradeHistoryRepository, OrderHistoryLookupRepository orderHistoryLookupRepository) {
        super(orderHistoryLookupRepository, tradeHistoryRepository);
        BinanceStreamProcessor.binanceLive = binanceLive;
        BinanceStreamProcessor.binanceLimitOrderRepository = binanceLimitOrderRepository;
    }

    public static synchronized void processRemoteOrder(ExecutionReportBinanceUserTransaction er) {
        LOG.info("Execution Report: {}", er);
        Order order = er.toOrder(false);
        if (er.getCommissionAsset() == null) {
            processRemoteOrder(order);
        } else {
            processRemoteOrder(order, Util.getCurrencyFromPortfolio(er.getCommissionAsset(), binanceLive.getPortfolio()), er.getCommissionAmount());
        }
    }

    public static synchronized void processRemoteOrder(Order order) {
        if (CONFIG.getBroker() == Broker.BINANCE_TEST) {
            final Currency bnb = Util.getCurrencyFromPortfolio("BNB", binanceLive.getPortfolio(), CurrencyType.CRYPTO);
            final CurrencyPair currencyPair = order.getInstrument() instanceof CurrencyPair ? ((CurrencyPair) order.getInstrument()) : null;
            if (currencyPair != null) {
                final BigDecimal bnbPrice = BinanceUtil.getTickerPrice(bnb.getSymbol(), currencyPair.base.getSymbol());
                final BigDecimal orderCommission = order.getOriginalAmount().multiply(CONFIG.getFee());
                processRemoteOrder(order, bnb, orderCommission.divide(bnbPrice,8, RoundingMode.HALF_UP));
            } else {
                processRemoteOrder(order, null, null);
            }
        } else {
            processRemoteOrder(order, null, null);
        }
    }

    public static synchronized void processRemoteOrder(Order order, Currency commissionAsset, BigDecimal commissionAmount) {
        LOG.info("Remote Order: {}", order);
        if (order instanceof LimitOrder limitOrder) {

            BinanceLimitOrder binanceLimitOrder = binanceLive.getPortfolio().getBinanceLimitOrders().stream().filter(blo -> blo.getId().equals(limitOrder.getId())).findFirst().orElse(null);

            if (binanceLimitOrder == null) {
                LOG.info("Couldn't find BinanceLimitOrder in Portfolio. Checking Repo...");
                binanceLimitOrder = binanceLimitOrderRepository.getById(limitOrder.getId());
            } else {
                LOG.info("Found BinanceLimitOrder in Portfolio");
            }

            if (binanceLimitOrder == null) {
                LOG.info("Couldn't find BinanceLimitOrder in Repo. Creating new...");
                binanceLimitOrder = new BinanceLimitOrder(limitOrder, binanceLive.getPortfolio());
                binanceLive.getPortfolio().getBinanceLimitOrders().add(binanceLimitOrder);
            }

            binanceLimitOrder.setCommissionAsset(commissionAsset);
            binanceLimitOrder.setCommissionAmount(commissionAmount);
            BinanceTransactions.updateBinanceLimitOrder(binanceLimitOrder, limitOrder);
            LOG.info("BinanceLimitOrder before it goes to processing: {}", binanceLimitOrder);
            BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
        }

        binanceLive.savePortfolio();
    }
}
