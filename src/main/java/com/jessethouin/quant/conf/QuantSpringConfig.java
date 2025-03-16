package com.jessethouin.quant.conf;

import net.jacobpeterson.alpaca.openapi.trader.model.Order;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Configuration
@EnableJpaRepositories("com.jessethouin.quant.*")
@EntityScan("com.jessethouin.quant.*")
public class QuantSpringConfig {
    private static final String BINANCE_LIMIT_ORDER_API = "http://localhost:8080/binance/limitOrder";
    private static final String BINANCE_STREAMING_LIMIT_ORDERS_API = "http://localhost:8080/binance/streamingLimitOrders";
    private static final String ALPACA_ORDER_API = "http://localhost:8080/alpaca/order";
    private static final String ALPACA_STREAMING_ORDERS_API = "http://localhost:8080/alpaca/streamingOrders";

    @Bean
    public WebClient binancePublishWebClient(){
        return WebClient.builder()
            .baseUrl(BINANCE_LIMIT_ORDER_API)
            .build();
    }

    @Bean
    public WebClient alpacaPublishWebClient(){
        return WebClient.builder()
            .baseUrl(ALPACA_ORDER_API)
            .build();
    }

    @Bean
    public WebClient binanceSubscribeWebClient(){
        return WebClient.builder()
            .baseUrl(BINANCE_STREAMING_LIMIT_ORDERS_API)
            .build();
    }

    @Bean
    public WebClient alpacaSubscribeWebClient(){
        return WebClient.builder()
            .baseUrl(ALPACA_STREAMING_ORDERS_API)
            .build();
    }

    @Bean
    public Sinks.Many<LimitOrder> binanceSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<LimitOrder> flux(Sinks.Many<LimitOrder> binanceSink){
        return binanceSink.asFlux();
    }

    @Bean
    public Sinks.Many<Order> alpacaSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<Order> alpacaFlux(Sinks.Many<Order> alpacaSink){
        return alpacaSink.asFlux();
    }

}
