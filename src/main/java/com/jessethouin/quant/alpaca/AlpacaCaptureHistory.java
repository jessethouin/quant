package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.beans.TradeHistory;
import com.jessethouin.quant.beans.repos.TradeHistoryRepository;
import net.jacobpeterson.alpaca.openapi.marketdata.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_CRYPTO_API;
import static com.jessethouin.quant.conf.Config.CONFIG;

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
                    String nextPageToken = null;
                    do {
                        CryptoBarsResp cryptoBarsResp = ALPACA_CRYPTO_API.cryptoBars(CryptoLoc.US, "BTC/USD", "1Min", OffsetDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), OffsetDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), null, nextPageToken, Sort.ASC);
                        nextPageToken = cryptoBarsResp.getNextPageToken();
                        cryptoBarsResp.getBars()
                                .forEach((_, cryptoBarList) -> cryptoBarList.forEach(cryptoBar -> {
                                    TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoBar.getT().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoBar.getC())).build();
                                    tradeHistories.add(tradeHistory);
                                }));
                    } while (nextPageToken != null);
                }
                case QUOTE -> {
                    long start = CONFIG.getBacktestStart().getTime();
                    long end = CONFIG.getBacktestEnd().getTime();
                    String nextPageToken = null;
                    do {
                        CryptoQuotesResp cryptoQuotesResponse = ALPACA_CRYPTO_API.cryptoQuotes(CryptoLoc.US, "BTC/USD", OffsetDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), OffsetDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), 10000L, nextPageToken, Sort.ASC);
                        nextPageToken = cryptoQuotesResponse.getNextPageToken();
                        cryptoQuotesResponse.getQuotes()
                                .forEach((_, cryptoQuoteList) -> cryptoQuoteList.forEach(cryptoQuote -> {
                                    TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoQuote.getT().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoQuote.getAp())).build();
                                    tradeHistories.add(tradeHistory);
                                }));
                    } while (nextPageToken != null);
                }
                case TRADE -> {
                    long start = CONFIG.getBacktestStart().getTime();
                    long end = CONFIG.getBacktestEnd().getTime();
                    String nextPageToken = null;
                    do {
                        CryptoTradesResp cryptoTradesResponse = ALPACA_CRYPTO_API.cryptoTrades(CryptoLoc.US, "BTC/USD", OffsetDateTime.ofInstant(Instant.ofEpochMilli(start), ZoneId.systemDefault()), OffsetDateTime.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault()), 10000L, nextPageToken, Sort.ASC);
                        nextPageToken = cryptoTradesResponse.getNextPageToken();
                        cryptoTradesResponse.getTrades()
                                .forEach((_, cryptoTradeList) -> cryptoTradeList.forEach(cryptoTrade -> {
                                    TradeHistory tradeHistory = TradeHistory.builder().timestamp(Date.from(cryptoTrade.getT().toInstant())).ma1(BigDecimal.ZERO).ma2(BigDecimal.ZERO).l(BigDecimal.ZERO).h(BigDecimal.ZERO).p(BigDecimal.valueOf(cryptoTrade.getP())).build();
                                    tradeHistories.add(tradeHistory);
                                }));
                    } while (nextPageToken != null);
                }
            }
            tradeHistoryRepository.saveAll(tradeHistories);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }
}
