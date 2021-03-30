package com.jessethouin.quant.alpaca;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import com.jessethouin.quant.alpaca.beans.repos.AlpacaOrderRepository;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Config;
import net.jacobpeterson.abstracts.websocket.exception.WebsocketException;
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
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.jessethouin.quant.conf.OrderStatus.FILLED;
import static net.jacobpeterson.polygon.websocket.message.PolygonStreamMessageType.AGGREGATE_PER_SECOND;
import static net.jacobpeterson.polygon.websocket.message.PolygonStreamMessageType.TRADE;

@Component
public class AlpacaLive {
    private static final Logger LOG = LogManager.getLogger(AlpacaLive.class);
    private static final Config config = Config.INSTANCE;
    private static Portfolio portfolio;
    private static final AlpacaAPI ALPACA_API = new AlpacaAPI();
    private static final PolygonAPI POLYGON_API = new PolygonAPI();

    private static AlpacaOrderRepository alpacaOrderRepository;
    private static PortfolioRepository portfolioRepository;

    private AlpacaLive(AlpacaOrderRepository alpacaOrderRepository, PortfolioRepository portfolioRepository) {
        AlpacaLive.alpacaOrderRepository = alpacaOrderRepository;
        AlpacaLive.portfolioRepository = portfolioRepository;
    }

    public static void doPaperTrading() {

        Account alpacaAccount = getAccount();

        if (alpacaAccount != null) {
            portfolio = portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc();

            if (portfolio == null) {
                portfolio = Util.createPortfolio();
                portfolioRepository.save(portfolio);
            }

            LOG.info("\n\nAlpaca Account Information:");
            LOG.info("\t" + alpacaAccount.toString().replace(",", ",\n\t"));

            LOG.info("Portfolio Cash:");
            portfolio.getCurrencies().forEach(c -> LOG.info("\t{} : {}", c.getSymbol(), c.getQuantity()));
            portfolio.getSecurities().forEach(security -> LOG.info("Open position:" + getOpenPosition(security.getSymbol())));

            Objects.requireNonNull(getClosedOrders()).forEach(o -> LOG.info("Closed Orders:" + o.toString()));

            openStreamListener(portfolio.getSecurities());
            try {
                openTradeUpdatesStream();
            } catch (WebsocketException e) {
                LOG.error("Unable to open websocket stream for trade (order) updates. {}", e.getLocalizedMessage());
            }
        }
    }

    private static Account getAccount() {
        Account alpacaAccount = null;
        // Get Account Information
        try {
            alpacaAccount = getAlpacaApi().getAccount();
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return alpacaAccount;
    }

    private static ArrayList<Order> getClosedOrders() {
        try {
            return getAlpacaApi().getOrders(OrderStatus.CLOSED, 500, ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS), ZonedDateTime.now(), Direction.ASCENDING, false);
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return null;
    }

    private static void openStreamListener(Set<Security> securities) {
        securities.forEach(s -> {
            try {
                getPolygonApi().addPolygonStreamListener(new PolygonStreamListenerAdapter(s.getSymbol(), PolygonStreamMessageType.values()) {
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

                        shortMAValue = Util.getMA(previousShortMAValue, config.getShortLookback(), price);
                        longMAValue = Util.getMA(previousLongMAValue, config.getLongLookback(), price);
                        c.updateCalc(price, shortMAValue, longMAValue);
                        c.decide();
                        LOG.debug(MessageFormat.format("{0,number,000} : ma1 {1,number,000.0000} : ma2 {2,number,000.0000} : l {3,number,000.0000}: h {4,number,000.0000}: p {5,number,000.0000} : {6,number,00000.0000}", count, shortMAValue, longMAValue, c.getLow(), c.getHigh(), price, Util.getPortfolioValue(portfolio, s.getCurrency(), price)));

                        portfolioRepository.save(portfolio);
                        previousShortMAValue = shortMAValue;
                        previousLongMAValue = longMAValue;
                        count++;
                    }
                });
            } catch (WebsocketException e) {
                LOG.error("Unable to open websocket stream for {}. {}", s.getSymbol(), e.getLocalizedMessage());
            }
        });
    }

    private static void openTradeUpdatesStream() throws WebsocketException {
        getAlpacaApi().addAlpacaStreamListener(new AlpacaStreamListenerAdapter(AlpacaStreamMessageType.TRADE_UPDATES, AlpacaStreamMessageType.ACCOUNT_UPDATES) {
            @Override
            public void onStreamUpdate(AlpacaStreamMessageType streamMessageType, AlpacaStreamMessage streamMessage) {
                switch (streamMessageType) {
                    case TRADE_UPDATES -> {
                        TradeUpdateMessage tradeUpdateMessage = (TradeUpdateMessage) streamMessage;
                        Order order = tradeUpdateMessage.getData().getOrder();
                        LOG.info(order.toString());
                        AlpacaOrder alpacaOrder = alpacaOrderRepository.getById(order.getId()); //todo: check for null. What if it didn't make it into the database on order creation?
                        Util.updateAlpacaOrder(alpacaOrder, order);
                        switch (com.jessethouin.quant.conf.OrderStatus.valueOf(alpacaOrder.getStatus())) {
                            case FILLED -> AlpacaTransactions.processFilledOrder(alpacaOrder);
                            case CANCELED -> LOG.info("Order canceled");
                            case EXPIRED -> LOG.info("Order expired");
                        }
                        if (alpacaOrder.getStatus().equals(FILLED.toString())) {
                            AlpacaTransactions.processFilledOrder(alpacaOrder);
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

    private static Position getOpenPosition(String symbol) {
        Position position = new Position();
        try {
            position = getAlpacaApi().getOpenPositionBySymbol(symbol);
        } catch (AlpacaAPIRequestException e) {
            LOG.error(e);
        }
        return position;
    }

    public static AlpacaAPI getAlpacaApi() {
        return ALPACA_API;
    }

    public static PolygonAPI getPolygonApi() {
        return POLYGON_API;
    }
}
