package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.conf.Instrument;
import lombok.Builder;
import lombok.Singular;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.bar.StockBarMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.quote.StockQuoteMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.stock.model.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataListener;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.stock.StockMarketDataListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_STOCK_STREAMING_API;
import static com.jessethouin.quant.common.StreamProcessor.processMarketData;

@Builder
public class AlpacaStockMarketSubscription {
    private static final Logger LOG = LogManager.getLogger(AlpacaStockMarketSubscription.class);
    @Singular
    private final List<Fundamental> fundamentals;
    boolean quotes;
    boolean trades;
    boolean bars;

    public void subscribe() {
        StockMarketDataListener stockMarketDataListener = new StockMarketDataListenerAdapter() {
            String symbol;
            Double price;
            Date timestamp;

            @Override
            public void onQuote(StockQuoteMessage quoteMessage) {
                symbol = quoteMessage.getSymbol();
                price = quoteMessage.getAskPrice();
                timestamp = Date.from(quoteMessage.getTimestamp().toInstant());
                LOG.debug("===> {} [{}]: {}", quoteMessage.getMessageType(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(quoteMessage.getTimestamp()), price);
                processStockMarketData();
            }

            @Override
            public void onTrade(StockTradeMessage tradeMessage) {
                symbol = tradeMessage.getSymbol();
                price = tradeMessage.getPrice();
                timestamp = Date.from(tradeMessage.getTimestamp().toInstant());
                LOG.debug("===> {} [{}]: {}", tradeMessage.getMessageType(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(tradeMessage.getTimestamp()), price);
                processStockMarketData();
            }

            @Override
            public void onMinuteBar(StockBarMessage barMessage) {
                symbol = barMessage.getSymbol();
                price = barMessage.getVwap();
                timestamp = Date.from(barMessage.getTimestamp().toInstant());
                LOG.debug("===> {} [{}]: {}", barMessage.getMessageType(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(barMessage.getTimestamp()), price);
                processStockMarketData();
            }

            private void processStockMarketData() {
                fundamentals.stream().filter(f -> f.getInstrument().equals(Instrument.STOCK) && f.getSecurity().getSymbol().equals(symbol)).findFirst().ifPresent(fundamental -> {
                    fundamental.setPrice(BigDecimal.valueOf(price));
                    fundamental.setTimestamp(timestamp);
                    processMarketData(fundamental);
                });
            }
        };

        ALPACA_STOCK_STREAMING_API.setListener(stockMarketDataListener);
        fundamentals.stream().filter(fundamental -> fundamental.getInstrument().equals(Instrument.STOCK)).forEach(fundamental -> {
            Set<String> stocks = Set.of(fundamental.getSecurity().getSymbol());
            if (trades) ALPACA_STOCK_STREAMING_API.setTradeSubscriptions(stocks);
            if (quotes) ALPACA_STOCK_STREAMING_API.setQuoteSubscriptions(stocks);
            if (bars) ALPACA_STOCK_STREAMING_API.setMinuteBarSubscriptions(stocks);
        });
    }
}