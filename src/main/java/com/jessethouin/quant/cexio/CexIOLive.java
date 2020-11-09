package com.jessethouin.quant.cexio;

import com.fasterxml.jackson.databind.ObjectMapper;
import info.bitrich.xchangestream.cexio.CexioStreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingExchangeFactory;
import io.reactivex.disposables.Disposable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;

import java.util.Comparator;
import java.util.List;

public class CexIOLive {
    private static final Logger LOG = LogManager.getLogger(CexIOLive.class);

    public static void main(String[] args) {
        String url = "wss://ws.cex.io/ws";
        String user = "up133530546";
        String key = "SGeoKNIsUqchpeXwolppGWeUEI";
        String secret = "Cvadx7qhFPr8q6roV3nmLFDyE";
        ObjectMapper objectMapper = new ObjectMapper();

        StreamingExchange exchange = StreamingExchangeFactory.INSTANCE.createExchange(CexioStreamingExchange.class);
        exchange.connect().blockingAwait();
        Disposable subscription2 = exchange.getStreamingMarketDataService()
                .getOrderBook(CurrencyPair.BTC_USD)
                .subscribe(orderBook -> {
                    LOG.info("Order book: {}", orderBook);
                    List<LimitOrder> asks = orderBook.getAsks();
                    asks.sort(Comparator.comparing(LimitOrder::getLimitPrice));
                    LOG.info("asks: {}", asks);
                });

    }
}
