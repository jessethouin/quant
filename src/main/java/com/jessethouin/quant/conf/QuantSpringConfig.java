package com.jessethouin.quant.conf;

import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Configuration
@EnableJpaRepositories("com.jessethouin.quant")
public class QuantSpringConfig {
    private static final String LIMIT_ORDER_API = "http://localhost:8080/limitOrder";
    private static final String STREAMING_LIMIT_ORDER_API = "http://localhost:8080/streamingLimitOrders";

    @Bean
    public WebClient publishWebClient(){
        return WebClient.builder()
            .baseUrl(LIMIT_ORDER_API)
            .build();
    }

    @Bean
    public WebClient subscribeWebClient(){
        return WebClient.builder()
            .baseUrl(STREAMING_LIMIT_ORDER_API)
            .build();
    }

    @Bean
    public Sinks.Many<LimitOrder> sink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<LimitOrder> flux(Sinks.Many<LimitOrder> sink){
        return sink.asFlux();
    }

}
