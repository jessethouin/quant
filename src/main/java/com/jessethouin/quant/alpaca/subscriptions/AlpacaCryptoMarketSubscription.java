package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.alpaca.AlpacaUtil;
import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.conf.Instrument;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.crypto.model.bar.CryptoBarMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.crypto.model.quote.CryptoQuoteMessage;
import net.jacobpeterson.alpaca.model.websocket.marketdata.streams.crypto.model.trade.CryptoTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.crypto.CryptoMarketDataListener;
import net.jacobpeterson.alpaca.websocket.marketdata.streams.crypto.CryptoMarketDataListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;

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
        CryptoMarketDataListener marketDataListener = new CryptoMarketDataListenerAdapter() {
            String counterSymbol;
            Double price;
            Date timestamp;

            @Override
            public void onQuote(CryptoQuoteMessage quoteMessage) {
                counterSymbol = AlpacaUtil.parseAlpacaCryptoSymbol(quoteMessage.getSymbol());
                price = quoteMessage.getAskPrice();
                timestamp = Date.from(quoteMessage.getTimestamp().toInstant());
                LOG.debug("===> " + quoteMessage.getMessageType() + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(quoteMessage.getTimestamp()) + "]: " + price);
                processCryptoMarketData();
            }

            @Override
            public void onTrade(CryptoTradeMessage tradeMessage) {
                counterSymbol = AlpacaUtil.parseAlpacaCryptoSymbol(tradeMessage.getSymbol());
                price = tradeMessage.getPrice();
                timestamp = Date.from(tradeMessage.getTimestamp().toInstant());
                LOG.debug("===> " + tradeMessage.getMessageType() + " [" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(tradeMessage.getTimestamp()) + "]: " + price);
                processCryptoMarketData();
            }

            @Override
            public void onMinuteBar(CryptoBarMessage barMessage) {
                counterSymbol = AlpacaUtil.parseAlpacaCryptoSymbol(barMessage.getSymbol());
                price = barMessage.getClose();
                timestamp = Date.from(barMessage.getTimestamp().toInstant());
                LOG.debug("===> {} [{}]: {}", barMessage.getMessageType(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z").format(barMessage.getTimestamp()), price);
                processCryptoMarketData();
            }

            private void processCryptoMarketData() {
                fundamentals.stream().filter(f -> f.getCounterCurrency().getSymbol().equals(counterSymbol)).findFirst().ifPresent(fundamental -> {
                    fundamental.setPrice(BigDecimal.valueOf(price));
                    fundamental.setTimestamp(timestamp);
                    processMarketData(fundamental);
                });
            }
        };

        ALPACA_CRYPTO_STREAMING_API.setListener(marketDataListener);
        fundamentals.stream().filter(fundamental -> fundamental.getInstrument().equals(Instrument.CRYPTO)).forEach(fundamental -> {
            Set<String> currencies = Set.of(fundamental.getCounterCurrency().getSymbol() + "/" + fundamental.getBaseCurrency().getSymbol());
            if (trades) ALPACA_CRYPTO_STREAMING_API.setTradeSubscriptions(currencies);
            if (quotes) ALPACA_CRYPTO_STREAMING_API.setQuoteSubscriptions(currencies);
            if (bars) ALPACA_CRYPTO_STREAMING_API.setMinuteBarSubscriptions(currencies);
        });
    }
}
