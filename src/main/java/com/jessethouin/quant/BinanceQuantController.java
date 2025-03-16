package com.jessethouin.quant;

import com.google.gson.*;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.binance.BinanceLive;
import com.jessethouin.quant.db.Exclude;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order;
import org.knowm.xchange.dto.Order.OrderStatus;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.jessethouin.quant.conf.Config.CONFIG;

@RestController()
public class BinanceQuantController {

    private static final Logger LOG = LogManager.getLogger(BinanceQuantController.class);
    private final PortfolioRepository portfolioRepository;
    private final BinanceLive binanceLive;
    private static final ExclusionStrategy EXCLUSION_STRATEGY;
    private static final Gson GSON;
    private final Flux<LimitOrder> binanceFlux;
    private final Sinks.Many<LimitOrder> binanceSink;

    static {
        EXCLUSION_STRATEGY = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }

            @Override
            public boolean shouldSkipField(FieldAttributes field) {
                return field.getAnnotation(Exclude.class) != null;
            }
        };
        GSON = new GsonBuilder()
            .addSerializationExclusionStrategy(EXCLUSION_STRATEGY)
            .registerTypeAdapter(OffsetDateTime.class, (JsonDeserializer<OffsetDateTime>) (json, _, _) -> OffsetDateTime.parse(json.getAsJsonPrimitive().getAsString()))
            .create();
    }

    public BinanceQuantController(PortfolioRepository portfolioRepository, BinanceLive binanceLive, Flux<LimitOrder> binanceFlux, Many<LimitOrder> binanceSink) {
        this.portfolioRepository = portfolioRepository;
        this.binanceLive = binanceLive;
        this.binanceFlux = binanceFlux;
        this.binanceSink = binanceSink;
    }

    @GetMapping(path = "/binance/triggerBuy", produces = "application/json")
    public @ResponseBody
    Double triggerBuy() {
        LOG.info("Triggering manual buy/bid.");
        CONFIG.setTriggerBuy(true);
        return (double) 0;
    }

    @GetMapping(path = "/binance/triggerSell", produces = "application/json")
    public @ResponseBody
    Double triggerSell() {
        LOG.info("Triggering manual sell/ask.");
        CONFIG.setTriggerSell(true);
        return (double) 0;
    }

    @GetMapping(path = "/binance/portfolio", produces = "application/json")
    public @ResponseBody
    String report() {
        return GSON.toJson(binanceLive.getPortfolio());
    }

    @GetMapping(path = "/binance/currencies", produces = "application/json")
    public @ResponseBody
    String currencies() {
        return GSON.toJson(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc().getCurrencies());
    }

    @GetMapping(value = "/binance/streamingLimitOrders", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<LimitOrder> getLimitOrdersStream(){
        return binanceFlux;
    }

    @PostMapping("/binance/limitOrder")
    public ResponseEntity<String> saveLimitOrder(@RequestBody String limitOrderString) {
        JsonObject jsonOrder = JsonParser.parseString(limitOrderString).getAsJsonObject();
        LOG.info(jsonOrder);

        final JsonElement type = jsonOrder.get("type");
        final JsonElement instrument = jsonOrder.get("instrument");
        if (type.isJsonNull() || instrument.isJsonNull()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(null);
        }

        final JsonElement limitPrice = jsonOrder.get("limitPrice");
        final JsonElement originalAmount = jsonOrder.get("originalAmount");
        final JsonElement cumulativeAmount = jsonOrder.get("cumulativeAmount");
        final JsonElement timestamp = jsonOrder.get("timestamp");
        final JsonElement orderStatus = jsonOrder.get("status");
        final JsonElement averagePrice = jsonOrder.get("averagePrice");
        final JsonElement userReference = jsonOrder.get("userReference");

        LimitOrder.Builder builder = new LimitOrder.Builder(OrderType.valueOf(type.getAsString()), new CurrencyPair(instrument.getAsString()));
        final LimitOrder limitOrder = builder
            .id(String.valueOf(new Date().getTime()))
            .limitPrice(limitPrice.isJsonNull() ? null : limitPrice.getAsBigDecimal())
            .originalAmount(originalAmount.isJsonNull() ? null : originalAmount.getAsBigDecimal())
            .cumulativeAmount(cumulativeAmount.isJsonNull() ? null : cumulativeAmount.getAsBigDecimal())
            .timestamp(timestamp.isJsonNull() ? null : new Date(timestamp.getAsLong()))
            .orderStatus(orderStatus.isJsonNull() ? null : OrderStatus.valueOf(orderStatus.getAsString()))
            .averagePrice(averagePrice.isJsonNull() ? null : averagePrice.getAsBigDecimal())
            .userReference(userReference.isJsonNull() ? null : userReference.getAsString())
            .build();

        fillTestLimitOrders(limitOrder);

        return ResponseEntity.ok(limitOrder.getId());
    }

    void fillTestLimitOrders(LimitOrder limitOrder) {
        new Timer("Receive Timer").schedule(new TimerTask() {
            @Override
            public void run() {
                binanceSink.tryEmitNext(limitOrder);
            }
        }, 250L);

        new Timer("Fill Timer").schedule(new TimerTask() {
            @Override
            public void run() {
                limitOrder.setOrderStatus(Order.OrderStatus.FILLED);
                limitOrder.setAveragePrice(BigDecimal.ONE);
                limitOrder.setCumulativeAmount(limitOrder.getOriginalAmount());

                binanceSink.tryEmitNext(limitOrder);
            }
        }, 750L);
    }
}
