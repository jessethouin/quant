package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
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
import net.jacobpeterson.alpaca.websocket.broker.listener.AlpacaStreamListenerAdapter;
import net.jacobpeterson.alpaca.websocket.broker.message.AlpacaStreamMessageType;
import net.jacobpeterson.domain.alpaca.account.Account;
import net.jacobpeterson.domain.alpaca.order.Order;
import net.jacobpeterson.domain.alpaca.position.Position;
import net.jacobpeterson.domain.alpaca.streaming.AlpacaStreamMessage;
import net.jacobpeterson.domain.alpaca.streaming.account.AccountUpdateMessage;
import net.jacobpeterson.domain.alpaca.streaming.trade.TradeUpdateMessage;
import net.jacobpeterson.domain.polygon.websocket.PolygonStreamMessage;
import net.jacobpeterson.domain.polygon.websocket.aggregate.AggregatePerSecondMessage;
import net.jacobpeterson.domain.polygon.websocket.trade.TradeMessage;
import net.jacobpeterson.polygon.PolygonAPI;
import net.jacobpeterson.polygon.websocket.listener.PolygonStreamListenerAdapter;
import net.jacobpeterson.polygon.websocket.message.PolygonStreamMessageType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

import static com.jessethouin.quant.conf.OrderStatus.*;
import static net.jacobpeterson.polygon.websocket.message.PolygonStreamMessageType.AGGREGATE_PER_SECOND;
import static net.jacobpeterson.polygon.websocket.message.PolygonStreamMessageType.TRADE;

public class AlpacaLive {
    private static final Logger LOG = LogManager.getLogger(AlpacaLive.class);
    private static final AlpacaLive instance = new AlpacaLive();
    private static final Config config = new Config();
    private static Portfolio portfolio;
    private final AlpacaAPI alpacaAPI = new AlpacaAPI();
    private final PolygonAPI polygonAPI = new PolygonAPI();

    private AlpacaLive(){}

    public static AlpacaLive getInstance(){
        return instance;
    }

    public static void doPaperTrading() {

        AlpacaLive alpacaLive = AlpacaLive.getInstance();
        Account alpacaAccount = alpacaLive.getAccount();

        if (alpacaAccount != null) {
            portfolio = Database.getPortfolio();

            if (portfolio == null) {
                portfolio = new Portfolio();
                portfolio.setCash(new BigDecimal(alpacaAccount.getCash()));
                List<String> tickers = config.getSecurities();
                tickers.forEach(t -> {
                    Security security = new Security();
                    security.setSymbol(t);
                    security.setPortfolio(portfolio);
                    portfolio.getSecurities().add(security);
                });
                Database.persistPortfolio(portfolio);
            }

            LOG.info("\n\nAlpaca Account Information:");
            LOG.info("\t" + alpacaAccount.toString().replace(",", ",\n\t"));

            LOG.info("Portfolion Cash: " + portfolio.getCash());
            portfolio.getSecurities().forEach(security -> LOG.info("Open position:" + alpacaLive.getOpenPosition(security.getSymbol())));

            Objects.requireNonNull(alpacaLive.getClosedOrders()).forEach(o -> LOG.info("Closed Orders:" + o.toString()));

            alpacaLive.openStreamListener(portfolio.getSecurities());
            alpacaLive.openTradeUpdatesStream();
        }
    }

    private Account getAccount() {
        Account alpacaAccount = null;
        // Get Account Information
        try {
            alpacaAccount = getAlpacaAPI().getAccount();
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return alpacaAccount;
    }

    private ArrayList<Order> getClosedOrders() {
        try {
            return getAlpacaAPI().getOrders(OrderStatus.CLOSED, 500, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS), ZonedDateTime.now(), Direction.ASCENDING, false);
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return null;
    }

    private void openStreamListener(List<Security> securities) {
        securities.forEach(s -> getPolygonAPI().addPolygonStreamListener(new PolygonStreamListenerAdapter(s.getSymbol(), PolygonStreamMessageType.values()) {
            final Calc c = new Calc(s, config, BigDecimal.ZERO);
            final List<BigDecimal> intradayPrices = new ArrayList<>();
            int count = 0;
            BigDecimal shortMAValue;
            BigDecimal longMAValue;
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal previousShortMAValue = BigDecimal.ZERO;
            BigDecimal previousLongMAValue = BigDecimal.ZERO;

            @Override
            public void onStreamUpdate(PolygonStreamMessageType streamMessageType, PolygonStreamMessage streamMessage) {
                switch (streamMessageType) {
                    case TRADE -> {
                        TradeMessage tradeMessage = (TradeMessage) streamMessage;
                        processMessage(TRADE, tradeMessage.getT(), tradeMessage.getP());
                    }
                    case AGGREGATE_PER_SECOND -> {
                        AggregatePerSecondMessage aggregatePerSecondMessage = (AggregatePerSecondMessage) streamMessage;
                        processMessage(AGGREGATE_PER_SECOND, aggregatePerSecondMessage.getE(), aggregatePerSecondMessage.getVw());
                    }
                }
            }

            private void processMessage(PolygonStreamMessageType streamMessageType, Long timestamp, Double p) {
                if (streamMessageType.equals(TRADE)) return;

                LOG.info("===> " + streamMessageType + " [" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date(timestamp)) + "]: " + p);
                price = BigDecimal.valueOf(p);
                if (count < config.getLongLookback()) intradayPrices.add(price);

                shortMAValue = Transactions.getMA(intradayPrices, previousShortMAValue, count, config.getShortLookback(), price);
                longMAValue = Transactions.getMA(intradayPrices, previousLongMAValue, count, config.getLongLookback(), price);
                c.updateCalc(price, shortMAValue, longMAValue, portfolio);
                try {
                    Transactions.addCash(portfolio, c.decide());
                    LOG.debug(MessageFormat.format("{0,number,000} : ma1 {1,number,000.0000} : ma2 {2,number,000.0000} : l {3,number,000.0000}: h {4,number,000.0000}: p {5,number,000.0000} : {6,number,00000.0000}", count, shortMAValue, longMAValue, c.getLow(), c.getHigh(), price, Transactions.getPortfolioValue(portfolio, s.getSymbol(), price)));
                } catch (CashException e) {
                    LOG.error(e.getLocalizedMessage());
                }

                Database.persistPortfolio(portfolio);
                previousShortMAValue = shortMAValue;
                previousLongMAValue = longMAValue;
                count++;
            }
        }));

        //RECEIVED: {"stream":"Q.AAPL","data":{"ev":"Q","T":"AAPL","x":17,"p":112.12,"s":1,"X":3,"P":112.98,"S":1,"c":[0],"t":1601645053026000000}}
/*
        securities.forEach(s -> alpacaAPI.addMarketDataStreamListener(new MarketDataStreamListenerAdapter(s.getSymbol(), TRADES, AGGREGATE_MINUTE) {
            final Calc c = new Calc(s, config, BigDecimal.ZERO);
            final List<BigDecimal> intradayPrices = new ArrayList<>();
            int count = 0;
            BigDecimal shortMAValue;
            BigDecimal longMAValue;
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal previousShortMAValue = BigDecimal.ZERO;
            BigDecimal previousLongMAValue = BigDecimal.ZERO;

            @Override
            public void onStreamUpdate(MarketDataStreamMessageType streamMessageType, MarketDataStreamMessage streamMessage) {
                switch (streamMessageType) {
                    case QUOTES -> {
                        QuoteMessage quoteMessage = (QuoteMessage) streamMessage;
                        LOG.info("\nQuote Update: \n\t" + quoteMessage.toString().replace(",", ",\n\t"));
                    }
                    case TRADES -> {
                        TradeMessage tradeMessage = (TradeMessage) streamMessage;
                        LOG.info("\nTrade Update [" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date(tradeMessage.getT() / 1000 / 1000)) + "]: \n\t" + tradeMessage.toString().replace(",", ",\n\t"));

                        price = BigDecimal.valueOf(tradeMessage.getP());
                        if (count < config.getLongLookback()) intradayPrices.add(price);

                        shortMAValue = Transactions.getMA(intradayPrices, previousShortMAValue, count, config.getShortLookback(), price);
                        longMAValue = Transactions.getMA(intradayPrices, previousLongMAValue, count, config.getLongLookback(), price);
                        c.updateCalc(price, shortMAValue, longMAValue, portfolio);
                        try {
                            Transactions.addCash(portfolio, c.decide());
                            LOG.debug(MessageFormat.format("{0,number,000} : ma1 {1,number,000.000} : ma2 {2,number,000.000} : l {3,number,000.000}: h {4,number,000.000}: p {5,number,000.000} : {6,number,00000.000}", count, shortMAValue, longMAValue, c.getLow(), c.getHigh(), price, Transactions.getPortfolioValue(portfolio, s.getSymbol(), price)));
                        } catch (CashException e) {
                            LOG.error(e.getLocalizedMessage());
                        }

                        Database.persistPortfolio(portfolio);
                        previousShortMAValue = shortMAValue;
                        previousLongMAValue = longMAValue;
                        count++;
                    }
                    case AGGREGATE_MINUTE -> {
                        AggregateMinuteMessage aggregateMessage = (AggregateMinuteMessage) streamMessage;
                        LOG.info("\nAggregate Minute Update: \n\t" + aggregateMessage.toString().replace(",", ",\n\t"));
                    }
                }
            }
        }));
*/
    }

    private void openTradeUpdatesStream() {
        getAlpacaAPI().addAlpacaStreamListener(new AlpacaStreamListenerAdapter(AlpacaStreamMessageType.TRADE_UPDATES, AlpacaStreamMessageType.ACCOUNT_UPDATES){
            @Override
            public void onStreamUpdate(AlpacaStreamMessageType streamMessageType, AlpacaStreamMessage streamMessage) {
                switch (streamMessageType) {
                    case TRADE_UPDATES -> {
                        TradeUpdateMessage tradeUpdateMessage = (TradeUpdateMessage) streamMessage;
                        Order thisOrder = tradeUpdateMessage.getData().getOrder();
                        LOG.info(thisOrder.toString());
                        AlpacaOrder thatOrder = Database.getAlpacaOrder(thisOrder.getId()); //todo: check for null. What if it didn't make it into the database on order creation?
                        if (!thatOrder.getStatus().equals(thisOrder.getStatus())) {
                            // update existing alpacaOrder
                        }
                        if (Stream.of(FILLED.getStatus(), EXPIRED.getStatus(), CANCELED.getStatus()).anyMatch(thisOrder.getStatus()::equalsIgnoreCase)) {
                            // remove alpacaa order
                        }
                    }
                    case ACCOUNT_UPDATES -> {
                        AccountUpdateMessage accountUpdateMessage = (AccountUpdateMessage) streamMessage;
                        LOG.info(accountUpdateMessage.getData().toString());
                    }
                }
            }
        });
    }

    private Position getOpenPosition(String symbol) {
        Position position = new Position();
        try {
            position = getAlpacaAPI().getOpenPositionBySymbol(symbol);
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return position;
    }

    public AlpacaAPI getAlpacaAPI() {
        return alpacaAPI;
    }

    public PolygonAPI getPolygonAPI() {
        return polygonAPI;
    }
}
