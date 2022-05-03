package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.alpaca.AlpacaStreamProcessor;
import lombok.Builder;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.streaming.enums.StreamingMessageType;
import net.jacobpeterson.alpaca.model.endpoint.streaming.trade.TradeUpdateMessage;
import net.jacobpeterson.alpaca.websocket.streaming.StreamingListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_STREAMING_API;
import static net.jacobpeterson.alpaca.model.endpoint.streaming.enums.StreamingMessageType.TRADE_UPDATES;

@Builder
public class AlpacaTradeUpdatesSubscription {
    private static final Logger LOG = LogManager.getLogger(AlpacaTradeUpdatesSubscription.class);

    public void subscribe() {
        StreamingListener streamingListener = (messageType, message) -> {
            if (messageType == TRADE_UPDATES) {
                TradeUpdateMessage tradeUpdateMessage = (TradeUpdateMessage) message;
                Order order = tradeUpdateMessage.getData().getOrder();
                LOG.info(order.toString());
                AlpacaStreamProcessor.processRemoteOrder(order);
            } else {
                LOG.info(message.toString());
            }
        };
        ALPACA_STREAMING_API.setListener(streamingListener);
        ALPACA_STREAMING_API.streams(StreamingMessageType.TRADE_UPDATES);
    }
}
