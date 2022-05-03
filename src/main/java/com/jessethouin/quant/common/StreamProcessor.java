package com.jessethouin.quant.common;

import com.jessethouin.quant.beans.OrderHistoryLookup;
import com.jessethouin.quant.beans.TradeHistory;
import com.jessethouin.quant.beans.repos.OrderHistoryLookupRepository;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Instruments;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

@Component
@Transactional
public class StreamProcessor {
    private static final Logger LOG = LogManager.getLogger(StreamProcessor.class);

    @Getter
    static OrderHistoryLookup orderHistoryLookup;
    static OrderHistoryLookupRepository orderHistoryLookupRepository;
    static TradeHistoryRepository tradeHistoryRepository;

    public StreamProcessor(OrderHistoryLookupRepository orderHistoryLookupRepository, TradeHistoryRepository tradeHistoryRepository) {
        StreamProcessor.orderHistoryLookupRepository = orderHistoryLookupRepository;
        StreamProcessor.tradeHistoryRepository = tradeHistoryRepository;
    }

    public static void processMarketData(Fundamental fundamental) {
        fundamental.setShortMAValue(Util.getMA(fundamental.getPreviousShortMAValue(), CONFIG.getShortLookback(), fundamental.getPrice()));
        fundamental.setLongMAValue(Util.getMA(fundamental.getPreviousLongMAValue(), CONFIG.getLongLookback(), fundamental.getPrice()));

        orderHistoryLookup = new OrderHistoryLookup();

        fundamental.getCalc().updateCalc(fundamental.getPrice(), fundamental.getShortMAValue(), fundamental.getLongMAValue());
        fundamental.getCalc().decide();

        TradeHistory tradeHistory = TradeHistory.builder()
                .timestamp(requireNonNullElse(fundamental.getTimestamp(), new Date()))
                .ma1(fundamental.getShortMAValue())
                .ma2(fundamental.getLongMAValue())
                .l(fundamental.getCalc().getLow())
                .h(fundamental.getCalc().getHigh())
                .p(fundamental.getPrice())
                .build();

        BigDecimal value;
        String symbol;
        if (fundamental.getInstrument().equals(Instruments.CRYPTO)) {
            symbol = fundamental.getCounterCurrency().getSymbol();
            value = Util.getValueAtPrice(fundamental.getCounterCurrency(), fundamental.getPrice()).add(fundamental.getBaseCurrency().getQuantity());
        } else {
            symbol = fundamental.getSecurity().getSymbol();
            value = Util.getValueAtPrice(fundamental.getSecurity(), fundamental.getPrice()).add(fundamental.getBaseCurrency().getQuantity());
        }

        orderHistoryLookup.setTradeId(tradeHistoryRepository.save(tradeHistory).getTradeId());
        orderHistoryLookup.setValue(value);
        orderHistoryLookupRepository.save(orderHistoryLookup);

        LOG.info("{}/{} - {} : ma1 {} : ma2 {} : l {} : h {} : p {} : v {}",
                symbol,
                fundamental.getBaseCurrency().getSymbol(),
                fundamental.getCount(),
                fundamental.getShortMAValue(),
                fundamental.getLongMAValue(),
                fundamental.getCalc().getLow(),
                fundamental.getCalc().getHigh(),
                fundamental.getPrice(),
                value);

        fundamental.setPreviousShortMAValue(fundamental.getShortMAValue());
        fundamental.setPreviousLongMAValue(fundamental.getLongMAValue());
        fundamental.setCount(fundamental.getCount() + 1);
    }

}
