package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.Portfolio;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.alpaca.websocket.marketdata.listener.MarketDataStreamListenerAdapter;
import net.jacobpeterson.alpaca.websocket.marketdata.message.MarketDataStreamMessageType;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.MarketDataStreamMessage;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.aggregate.AggregateMinuteMessage;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.quote.QuoteMessage;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.trade.TradeMessage;
import net.jacobpeterson.domain.alpaca.position.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;

import static net.jacobpeterson.alpaca.websocket.marketdata.message.MarketDataStreamMessageType.*;

public class Live {
    private static final Logger LOG = LogManager.getLogger(Live.class);
    AlpacaAPI alpacaAPI = new AlpacaAPI();

    public static void doPaperTrading(String[] args) {
        Live live = new Live();
        Account alpacaAccount = live.getAccount();

        if (alpacaAccount != null) {
            Portfolio portfolio = new Portfolio();
            portfolio.setCash(new BigDecimal(alpacaAccount.getCash()));

            LOG.info("\n\nAccount Information:");
            LOG.info("\t" + alpacaAccount.toString().replace(",", ",\n\t"));

            LOG.info("Portfolion Cash: " + portfolio.getCash());
            LOG.info(live.getOpenPosition("AAPL"));

            live.openQuoteListener();
        }
    }

    private Account getAccount() {
        Account alpacaAccount = null;
        // Get Account Information
        try {
            alpacaAccount = alpacaAPI.getAccount();
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return alpacaAccount;
    }

    private void openQuoteListener() {
        alpacaAPI.addMarketDataStreamListener(new MarketDataStreamListenerAdapter("AAPL", QUOTES, TRADES, AGGREGATE_MINUTE) {
            @Override
            public void onStreamUpdate(MarketDataStreamMessageType streamMessageType, MarketDataStreamMessage streamMessage) {
                switch (streamMessageType) {
                    case QUOTES -> {
                        QuoteMessage quoteMessage = (QuoteMessage) streamMessage;
                        LOG.info("\nQuote Update: \n\t" + quoteMessage.toString().replace(",", ",\n\t"));
                    }
                    case TRADES -> {
                        TradeMessage tradeMessage = (TradeMessage) streamMessage;
                        LOG.info("\nTrade Update: \n\t" + tradeMessage.toString().replace(",", ",\n\t"));
                    }
                    case AGGREGATE_MINUTE -> {
                        AggregateMinuteMessage aggregateMessage = (AggregateMinuteMessage) streamMessage;
                        LOG.info("\nAggregate Minute Update: \n\t" + aggregateMessage.toString().replace(",", ",\n\t"));
                    }
                }
            }
        });
    }

    private Position getOpenPosition(String symbol) {
        Position position = new Position();
        try {
            position = alpacaAPI.getOpenPositionBySymbol(symbol);
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return position;
    }

}
