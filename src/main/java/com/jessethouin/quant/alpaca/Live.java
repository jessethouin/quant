package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.db.Database;
import com.jessethouin.quant.exceptions.CashException;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.enums.Direction;
import net.jacobpeterson.alpaca.enums.OrderStatus;
import net.jacobpeterson.alpaca.rest.exception.AlpacaAPIRequestException;
import net.jacobpeterson.alpaca.websocket.marketdata.listener.MarketDataStreamListenerAdapter;
import net.jacobpeterson.alpaca.websocket.marketdata.message.MarketDataStreamMessageType;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.MarketDataStreamMessage;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.aggregate.AggregateMinuteMessage;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.quote.QuoteMessage;
import net.jacobpeterson.domain.alpaca.marketdata.streaming.trade.TradeMessage;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.domain.alpaca.position.Position;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static net.jacobpeterson.alpaca.websocket.marketdata.message.MarketDataStreamMessageType.AGGREGATE_MINUTE;
import static net.jacobpeterson.alpaca.websocket.marketdata.message.MarketDataStreamMessageType.TRADES;

public class Live {
    private static final Logger LOG = LogManager.getLogger(Live.class);
    private static final Config config = new Config();
    private static Portfolio portfolio;
    final AlpacaAPI alpacaAPI = new AlpacaAPI();

    public static void doPaperTrading() {

        Live live = new Live();
        Account alpacaAccount = live.getAccount();

        if (alpacaAccount != null) {
            portfolio = Database.get();
            if (portfolio == null) {
                portfolio = new Portfolio();
                portfolio.setCash(new BigDecimal(alpacaAccount.getCash()));
            }

            List<Security> securities = new ArrayList<>();
            List<String> tickers = Arrays.asList("GOOG", "AAPL");
            tickers.forEach(t -> {
                Security security = new Security();
                security.setSymbol(t);
                security.setPortfolio(portfolio);
                securities.add(security);
            });

            portfolio.setSecurities(securities);

            LOG.info("\n\nAccount Information:");
            LOG.info("\t" + alpacaAccount.toString().replace(",", ",\n\t"));

            LOG.info("Portfolion Cash: " + portfolio.getCash());
            portfolio.getSecurities().forEach(security -> LOG.info(live.getOpenPosition(security.getSymbol())));

            Objects.requireNonNull(live.getClosedOrders()).forEach(o -> LOG.info(o.toString()));

            live.openStreamListener(portfolio.getSecurities());
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

    private ArrayList<Order> getClosedOrders() {
        try {
            return alpacaAPI.getOrders(OrderStatus.CLOSED, 500, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS), ZonedDateTime.now(), Direction.ASCENDING, false);
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return null;
    }

    private void openStreamListener(List<Security> securities) {
        //RECEIVED: {"stream":"Q.AAPL","data":{"ev":"Q","T":"AAPL","x":17,"p":112.12,"s":1,"X":3,"P":112.98,"S":1,"c":[0],"t":1601645053026000000}}
        securities.forEach(s -> alpacaAPI.addMarketDataStreamListener(new MarketDataStreamListenerAdapter(s.getSymbol(), TRADES, AGGREGATE_MINUTE) {
            final Calc c = new Calc(s, config, BigDecimal.ZERO);
            final List<BigDecimal> intradayPrices = new ArrayList<>();
            int count = 0;
            BigDecimal sv;
            BigDecimal lv;
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal previous = BigDecimal.ZERO;

            @Override
            public void onStreamUpdate(MarketDataStreamMessageType streamMessageType, MarketDataStreamMessage streamMessage) {
                switch (streamMessageType) {
                    case QUOTES -> {
                        QuoteMessage quoteMessage = (QuoteMessage) streamMessage;
//                        LOG.info("\nQuote Update: \n\t" + quoteMessage.toString().replace(",", ",\n\t"));
                    }
                    case TRADES -> {
                        TradeMessage tradeMessage = (TradeMessage) streamMessage;
                        LOG.info("\nTrade Update: \n\t" + tradeMessage.toString().replace(",", ",\n\t"));

                        price = BigDecimal.valueOf(tradeMessage.getP());
                        if (count < config.getLongLookback()) intradayPrices.add(price);

                        sv = Transactions.getMA(intradayPrices, previous, count, config.getShortLookback(), price);
                        lv = Transactions.getMA(intradayPrices, previous, count, config.getLongLookback(), price);
                        c.updateCalc(price, sv, lv, portfolio);
                        try {
                            Transactions.addCash(portfolio, c.decide());
                            LOG.debug(MessageFormat.format("{8,number,000} : {0,number,00} : {5,number,000.000} : {1,number,00} : {6,number,000.000} : {7,number,000.000} : {2,number,0.00} : {3,number,0.00} : {4,number,00000.000}", config.getShortLookback(), config.getLongLookback(), config.getLowRisk(), config.getHighRisk(), Transactions.getPortfolioValue(portfolio, s.getSymbol(), price), sv, lv, price, count));
                        } catch (CashException e) {
                            LOG.error(e.getLocalizedMessage());
                        }

                        previous = price;
                        count++;
                    }
                    case AGGREGATE_MINUTE -> {
                        AggregateMinuteMessage aggregateMessage = (AggregateMinuteMessage) streamMessage;
                        LOG.info("\nAggregate Minute Update: \n\t" + aggregateMessage.toString().replace(",", ",\n\t"));
                    }
                }
            }
        }));
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
