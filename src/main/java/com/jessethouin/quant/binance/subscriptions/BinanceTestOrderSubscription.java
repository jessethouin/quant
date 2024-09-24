package com.jessethouin.quant.binance.subscriptions;

import com.jessethouin.quant.binance.BinanceStreamProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

@Component
public class BinanceTestOrderSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceTestOrderSubscription.class);
    private final WebClient binanceSubscribeWebClient;

    public BinanceTestOrderSubscription(WebClient binanceSubscribeWebClient) {
        this.binanceSubscribeWebClient = binanceSubscribeWebClient;
    }

    public Disposable subscribe() {
        return binanceSubscribeWebClient.get().retrieve().bodyToFlux(LimitOrder.class).subscribe(BinanceStreamProcessor::processRemoteOrder, throwable -> LOG.error("Error in test order subscription", throwable));
    }
}
