package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.alpaca.beans.repos.AlpacaOrderRepository;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.OrderHistoryLookup;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.repos.OrderHistoryLookupRepository;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.common.StreamProcessor;
import com.jessethouin.quant.conf.CurrencyTypes;
import lombok.Getter;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@Transactional
public class AlpacaStreamProcessor extends StreamProcessor {
    private static final Logger LOG = LogManager.getLogger(AlpacaStreamProcessor.class);

    @Getter
    static AlpacaLive alpacaLive;
    static OrderHistoryLookup orderHistoryLookup;
    static OrderHistoryLookupRepository orderHistoryLookupRepository;
    static AlpacaOrderRepository alpacaOrderRepository;
    static TradeHistoryRepository tradeHistoryRepository;

    public AlpacaStreamProcessor(AlpacaLive alpacaLive, OrderHistoryLookupRepository orderHistoryLookupRepository, AlpacaOrderRepository alpacaOrderRepository, TradeHistoryRepository tradeHistoryRepository) {
        super(orderHistoryLookupRepository, tradeHistoryRepository);
        AlpacaStreamProcessor.alpacaLive = alpacaLive;
        AlpacaStreamProcessor.alpacaOrderRepository = alpacaOrderRepository;
    }

    public static synchronized void processRemoteOrder(Order order) {
        AlpacaOrder alpacaOrder = reconcileRemoteOrder(order);
        Portfolio portfolio = alpacaLive.getPortfolio();
        BigDecimal limitPrice = new BigDecimal(ObjectUtils.defaultIfNull(alpacaOrder.getLimitPrice(), "0"));
        BigDecimal bidAskQty = new BigDecimal(ObjectUtils.defaultIfNull(alpacaOrder.getQty(), "0"));
        BigDecimal filledQty = new BigDecimal(ObjectUtils.defaultIfNull(alpacaOrder.getFilledQty(), "0"));
        BigDecimal filledAvgPrice = new BigDecimal(ObjectUtils.defaultIfNull(alpacaOrder.getFilledAvgPrice(), "0"));

        switch (alpacaOrder.getAssetClass()) {
            case "crypto" -> {
                String symbol = AlpacaUtil.parseAlpacaCryptoSymbol(alpacaOrder.getSymbol());
                Currency counter = Util.getCurrencyFromPortfolio(symbol, portfolio, CurrencyTypes.CRYPTO);
                Currency base = Util.getCurrencyFromPortfolio("USD", portfolio, CurrencyTypes.FIAT);
                processRemoteCryptoOrder(alpacaOrder, counter, base, limitPrice, bidAskQty, filledQty, filledAvgPrice);
            }
            case "us_equity" -> {
                Security security = Util.getSecurityFromPortfolio(alpacaOrder.getSymbol(), portfolio);
                processRemoteSecurityOrder(alpacaOrder, security, limitPrice, bidAskQty, filledQty, filledAvgPrice);
            }
        }
        alpacaLive.savePortfolio();
    }

    private static void processRemoteCryptoOrder(AlpacaOrder alpacaOrder, Currency counter, Currency base, BigDecimal limitPrice, BigDecimal bidAskQty, BigDecimal filledQty, BigDecimal filledAvgPrice) {
        switch (alpacaOrder.getStatus()) {
            case NEW -> {
                switch (alpacaOrder.getSide()) {
                    case BUY -> Util.debit(base, bidAskQty.multiply(limitPrice), "New buy order " + alpacaOrder.getId());
                    case SELL -> Util.debit(counter, bidAskQty, "New sell order " + alpacaOrder.getId());
                }
            }
            case FILLED -> {
                switch (alpacaOrder.getSide()) {
                    case BUY -> Util.credit(counter, filledQty, "Filled buy order " + alpacaOrder.getId());
                    case SELL -> Util.credit(base, filledQty.multiply(filledAvgPrice), "Filled sell order " + alpacaOrder.getId());
                }
            }
            case CANCELED, EXPIRED -> {
                switch (alpacaOrder.getSide()) {
                    case BUY -> Util.credit(base, bidAskQty.multiply(limitPrice), alpacaOrder.getStatus() + " buy order " + alpacaOrder.getId());
                    case SELL -> Util.credit(counter, bidAskQty, alpacaOrder.getStatus() + " sell order " + alpacaOrder.getId());
                }
            }
        }
    }

    private static void processRemoteSecurityOrder(AlpacaOrder alpacaOrder, Security security, BigDecimal limitPrice, BigDecimal bidAskQty, BigDecimal filledQty, BigDecimal filledAvgPrice) {
        switch (alpacaOrder.getStatus()) {
            case NEW -> {
                switch (alpacaOrder.getSide()) {
                    case BUY -> Util.debit(security.getCurrency(), bidAskQty.multiply(limitPrice), "Buying Alpaca Security");
                    case SELL -> Transactions.adjustSecurityPosition(security, bidAskQty.negate(), limitPrice);
                }
            }
            case FILLED -> {
                switch (alpacaOrder.getSide()) {
                    case BUY -> Transactions.adjustSecurityPosition(security, filledQty, filledAvgPrice);
                    case SELL -> Util.credit(security.getCurrency(), filledQty.multiply(filledAvgPrice), "Selling Alpaca Security");
                }
            }
            case CANCELED, EXPIRED -> {
                switch (alpacaOrder.getSide()) {
                    case BUY -> Util.credit(security.getCurrency(), bidAskQty.multiply(limitPrice), alpacaOrder.getStatus() + " Alpaca Security order");
                    case SELL -> Transactions.adjustSecurityPosition(security, bidAskQty, limitPrice);
                }
            }
        }
    }

    public static synchronized AlpacaOrder reconcileRemoteOrder(Order order) {
        LOG.info("Remote Order: {}", order);

        Optional<AlpacaOrder> alpacaOrderOptional = alpacaLive.getPortfolio().getAlpacaOrders().stream().filter(alpacaOrder -> alpacaOrder.getId().equals(order.getId())).findFirst();
        AlpacaOrder alpacaOrder;

        if (alpacaOrderOptional.isPresent()) {
            LOG.info("Found AlpacaOrder in Portfolio");
            alpacaOrder = alpacaOrderOptional.get();
        } else {
            LOG.info("Couldn't find AlpacaOrder in Portfolio. Checking Repo...");
            alpacaOrder = alpacaOrderRepository.getById(order.getId());

            if (alpacaOrder == null) {
                LOG.info("Couldn't find AlpacaOrder in Repo. Creating new...");
                alpacaOrder = new AlpacaOrder(order, alpacaLive.getPortfolio());
            }
            alpacaLive.getPortfolio().getAlpacaOrders().add(alpacaOrder);
        }
        AlpacaTransactions.updateAlpacaOrder(alpacaOrder, order);
        alpacaLive.savePortfolio();
        return alpacaOrder;
    }

    public static void processFilledSecurityOrder(AlpacaOrder alpacaOrder) {
        Portfolio portfolio = alpacaOrder.getPortfolio();
        Security security = Util.getSecurityFromPortfolio(alpacaOrder.getSymbol(), portfolio);
        BigDecimal filledQty = new BigDecimal(alpacaOrder.getFilledQty());
        BigDecimal filledAvgPrice = new BigDecimal(alpacaOrder.getFilledAvgPrice());

        if (alpacaOrder.getSide().equals(OrderSide.BUY)) {
            Transactions.adjustSecurityPosition(security, filledQty, filledAvgPrice);
            Util.debit(security.getCurrency(), filledQty.multiply(filledAvgPrice).negate(), "Buying Alpaca Security");
        }
        if (alpacaOrder.getSide().equals(OrderSide.SELL)) {
            Util.credit(security.getCurrency(), filledQty.multiply(filledAvgPrice), "Selling Alpaca Security");
            Transactions.adjustSecurityPosition(security, filledQty, filledAvgPrice);
        }
    }
}
