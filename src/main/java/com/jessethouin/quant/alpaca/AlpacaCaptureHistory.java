package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.beans.TradeHistory;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.quote.CryptoQuote;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.quote.CryptoQuotesResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTrade;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTradesResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_CRYPTO_API;
import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.concurrent.TimeUnit.MINUTES;

@Component
public class AlpacaCaptureHistory {
    private static final Logger LOG = LogManager.getLogger(AlpacaCaptureHistory.class);
    static TradeHistoryRepository tradeHistoryRepository;

    public AlpacaCaptureHistory(TradeHistoryRepository tradeHistoryRepository) {
        AlpacaCaptureHistory.tradeHistoryRepository = tradeHistoryRepository;
    }

    public void doCapture() {
        try {
            List<TradeHistory> tradeHistories = new ArrayList<>();

            switch (CONFIG.getDataFeed()) {
                case BAR -> {
                    long start = CONFIG.getBacktestStart().getTime();
                    long end = CONFIG.getBacktestEnd().getTime();
                    long qty = end - start; //milliseconds between start and end dates/times
                    for (long i = qty; i > -1; i -= MINUTES.toMillis(500)) {
                        long e = start + Math.min(MINUTES.toMillis(500), i);
                        ALPACA_CRYPTO_API.getBars("BTCUSD", List.of(Exchange.COINBASE), ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), 500, null, 1, BarTimePeriod.MINUTE).getBars().forEach(cryptoBar -> {
                            TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoBar.getTimestamp().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoBar.getClose())).build();
                            tradeHistories.add(tradeHistory);
                        });
                        start = e + 1;
                    }
                }
                case QUOTE -> {
                    long start = CONFIG.getBacktestStart().getTime();
                    long end = CONFIG.getBacktestEnd().getTime();
                    CryptoQuotesResponse cryptoQuotesResponse = ALPACA_CRYPTO_API.getQuotes("BTCUSD", List.of(Exchange.COINBASE), ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), 10000, null);
                    String nextPageToken = cryptoQuotesResponse.getNextPageToken();
                    ArrayList<CryptoQuote> quotes = cryptoQuotesResponse.getQuotes();

                    while (nextPageToken != null) {
                        quotes.forEach(cryptoQuote -> {
                            TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoQuote.getTimestamp().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoQuote.getAskPrice())).build();
                            tradeHistories.add(tradeHistory);
                        });
                        cryptoQuotesResponse = ALPACA_CRYPTO_API.getQuotes("BTCUSD", List.of(Exchange.COINBASE), ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), 10000, nextPageToken);
                        nextPageToken = cryptoQuotesResponse.getNextPageToken();
                        quotes = cryptoQuotesResponse.getQuotes();
                    }

                    quotes.forEach(cryptoQuote -> {
                        TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoQuote.getTimestamp().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoQuote.getAskPrice())).build();
                        tradeHistories.add(tradeHistory);
                    });
                }
                case TRADE -> {
                    long start = CONFIG.getBacktestStart().getTime();
                    long end = CONFIG.getBacktestEnd().getTime();
                    CryptoTradesResponse cryptoTradesResponse = ALPACA_CRYPTO_API.getTrades("BTCUSD", List.of(Exchange.COINBASE), ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), 10000, null);
                    String nextPageToken = cryptoTradesResponse.getNextPageToken();
                    ArrayList<CryptoTrade> trades = cryptoTradesResponse.getTrades();

                    while (nextPageToken != null) {
                        trades.forEach(cryptoTrade -> {
                            TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoTrade.getTimestamp().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoTrade.getPrice())).build();
                            tradeHistories.add(tradeHistory);
                        });
                        cryptoTradesResponse = ALPACA_CRYPTO_API.getTrades("BTCUSD", List.of(Exchange.COINBASE), ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), ZonedDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), 10000, nextPageToken);
                        nextPageToken = cryptoTradesResponse.getNextPageToken();
                        trades = cryptoTradesResponse.getTrades();
                    }

                    trades.forEach(cryptoTrade -> {
                        TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoTrade.getTimestamp().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoTrade.getPrice())).build();
                        tradeHistories.add(tradeHistory);
                    });
                }
            }
            tradeHistoryRepository.saveAll(tradeHistories);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

}
