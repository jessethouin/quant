package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.alpaca.AlpacaStreamProcessor;
import lombok.Builder;
import net.jacobpeterson.alpaca.openapi.trader.model.Order;
import net.jacobpeterson.alpaca.websocket.updates.UpdatesListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.jessethouin.quant.alpaca.config.AlpacaApiServices.ALPACA_STREAMING_API;

@Builder
public class AlpacaTradeUpdatesSubscription {
    private static final Logger LOG = LogManager.getLogger(AlpacaTradeUpdatesSubscription.class);

    public void subscribe() {
        UpdatesListener updatesListener = tradeUpdate -> {
            Order order = tradeUpdate.getData().getOrder();
            LOG.info("Incoming order {} status {}", order.getId(), order.getStatus());
            AlpacaStreamProcessor.processRemoteOrder(order);
        };

        ALPACA_STREAMING_API.setListener(updatesListener);
        ALPACA_STREAMING_API.subscribeToTradeUpdates(true);
    }
}
