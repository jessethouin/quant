package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.alpaca.AlpacaUtil;
import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.conf.Instruments;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.bar.CryptoBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.quote.CryptoQuoteMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.trade.CryptoTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_CRYPTO_STREAMING_API;
import static com.jessethouin.quant.common.StreamProcessor.processMarketData;

@Getter
@Builder
public class AlpacaCryptoMarketSubscription {
    private static final Logger LOG = LogManager.getLogger(AlpacaCryptoMarketSubscription.class);
    @Singular private final List<Fundamental> fundamentals;
    boolean quotes;
    boolean trades;
    boolean bars;

    public void subscribe() {
        MarketDataListener marketDataListener = (messageType, message) -> {
            String counterSymbol;
            Double price;
            Date timestamp;
            switch (messageType) {
                case QUOTE -> {
                    CryptoQuoteMessage quoteMessage = (CryptoQuoteMessage) message;
                    counterSymbol = AlpacaUtil.parseAlpacaCryptoSymbol(quoteMessage.getSymbol());
                    price = quoteMessage.getAskPrice();
                    timestamp = Date.from(quoteMessage.getTimestamp().toInstant());
                    LOG.info("===> " + messageType + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(quoteMessage.getTimestamp()) + "]: " + quoteMessage.getAskPrice());
                }
                case TRADE -> {
                    CryptoTradeMessage tradeMessage = (CryptoTradeMessage) message;
                    counterSymbol = AlpacaUtil.parseAlpacaCryptoSymbol(tradeMessage.getSymbol());
                    price = tradeMessage.getPrice();
                    timestamp = Date.from(tradeMessage.getTimestamp().toInstant());
                    LOG.info("===> " + messageType + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(tradeMessage.getTimestamp()) + "]: " + tradeMessage.getPrice());
                }
                case BAR -> {
                    CryptoBarMessage barMessage = (CryptoBarMessage) message;
                    counterSymbol = AlpacaUtil.parseAlpacaCryptoSymbol(barMessage.getSymbol());
                    price = barMessage.getVwap();
                    timestamp = Date.from(barMessage.getTimestamp().toInstant());
                    LOG.info("===> " + messageType + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(barMessage.getTimestamp()) + "]: " + barMessage.getVwap());
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> {
                    LOG.info("===> " + messageType + " [" + message.toString() + "]");
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaCryptoMarketSubscription.subscribe()");
            }
            fundamentals.stream().filter(f -> f.getCounterCurrency().getSymbol().equals(counterSymbol)).findFirst().ifPresent(fundamental -> {
                fundamental.setPrice(BigDecimal.valueOf(price));
                fundamental.setTimestamp(timestamp);
                processMarketData(fundamental);
            });
        };

        ALPACA_CRYPTO_STREAMING_API.setListener(marketDataListener);
        fundamentals.stream().filter(fundamental -> fundamental.getInstrument().equals(Instruments.CRYPTO)).forEach(fundamental -> {
            List<String> currencies = List.of(fundamental.getCounterCurrency().getSymbol() + fundamental.getBaseCurrency().getSymbol());
            ALPACA_CRYPTO_STREAMING_API.subscribe(trades ? currencies : null, quotes ? currencies : null, bars ? currencies : null);
        });
    }
}
