package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.conf.Instrument;
import lombok.Builder;
import lombok.Singular;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.bar.StockBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.quote.StockQuoteMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

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
        MarketDataListener marketDataListener = (messageType, message) -> {
            String symbol;
            Double price;
            Date timestamp;
            switch (messageType) {
                case QUOTE -> {
                    StockQuoteMessage quoteMessage = (StockQuoteMessage) message;
                    symbol = quoteMessage.getSymbol();
                    price = quoteMessage.getAskPrice();
                    timestamp = Date.from(quoteMessage.getTimestamp().toInstant());
                    LOG.info("===> " + messageType + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(quoteMessage.getTimestamp()) + "]: " + quoteMessage.getAskPrice());
                }
                case TRADE -> {
                    StockTradeMessage tradeMessage = (StockTradeMessage) message;
                    symbol = tradeMessage.getSymbol();
                    price = tradeMessage.getPrice();
                    timestamp = Date.from(tradeMessage.getTimestamp().toInstant());
                    LOG.info("===> " + messageType + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(tradeMessage.getTimestamp()) + "]: " + tradeMessage.getPrice());
                }
                case BAR -> {
                    StockBarMessage barMessage = (StockBarMessage) message;
                    symbol = barMessage.getSymbol();
                    price = barMessage.getVwap();
                    timestamp = Date.from(barMessage.getTimestamp().toInstant());
                    LOG.info("===> " + messageType + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(barMessage.getTimestamp()) + "]: " + barMessage.getVwap());
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> {
                    LOG.info("===> " + messageType + " [" + message.toString() + "]");
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaStockMarketSubscription.subscribe()");
            }
            fundamentals.stream().filter(f -> f.getInstrument().equals(Instrument.STOCK) && f.getSecurity().getSymbol().equals(symbol)).findFirst().ifPresent(fundamental -> {
                fundamental.setPrice(BigDecimal.valueOf(price));
                fundamental.setTimestamp(timestamp);
                processMarketData(fundamental);
            });
        };

        ALPACA_STOCK_STREAMING_API.setListener(marketDataListener);
        fundamentals.stream().filter(fundamental -> fundamental.getInstrument().equals(Instrument.STOCK)).forEach(fundamental -> {
            List<String> stocks = List.of(fundamental.getSecurity().getSymbol());
            ALPACA_STOCK_STREAMING_API.subscribe(trades ? stocks : null, quotes ? stocks : null, bars ? stocks : null);
        });
    }
}