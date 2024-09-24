package com.jessethouin.quant.alpaca.subscriptions;

import com.jessethouin.quant.alpaca.AlpacaStreamProcessor;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

@Component
public class AlpacaTestTradeUpdatesSubscription {
    private static final Logger LOG = LogManager.getLogger(AlpacaTestTradeUpdatesSubscription.class);
    private final WebClient alpacaSubscribeWebClient;

    public AlpacaTestTradeUpdatesSubscription(WebClient alpacaSubscribeWebClient) {
        this.alpacaSubscribeWebClient = alpacaSubscribeWebClient;
    }

    /**
     * Subscribes to the feed accepting new Orders, mimicking Alpaca server
     * @return The subscription
     */
    public Disposable subscribe() {
        return alpacaSubscribeWebClient
                .get()
                .retrieve()
                .bodyToFlux(Order.class)
                .subscribe(AlpacaStreamProcessor::processRemoteOrder, throwable -> LOG.error("Error in test order subscription"));
    }
}
