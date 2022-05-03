package com.jessethouin.quant;

import com.google.gson.*;
import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.db.Exclude;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderStatus;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.jessethouin.quant.conf.Config.CONFIG;

@RestController
public class AlpacaQuantController {

    private static final Logger LOG = LogManager.getLogger(AlpacaQuantController.class);
    private final PortfolioRepository portfolioRepository;
    private final AlpacaLive alpacaLive;
    private final Flux<Order> alpacaFlux;
    private final Sinks.Many<Order> alpacaSink;
    private static final ExclusionStrategy EXCLUSION_STRATEGY;
    private static final Gson GSON;

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy - HH:mm:ss Z");
        GSON = new GsonBuilder()
                .addSerializationExclusionStrategy(EXCLUSION_STRATEGY)
                .registerTypeAdapter(ZonedDateTime.class, (JsonDeserializer<ZonedDateTime>) (json, type, jsonDeserializationContext) -> ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()))
                .create();
    }

    public AlpacaQuantController(PortfolioRepository portfolioRepository, AlpacaLive alpacaLive, Flux<Order> alpacaFlux, Sinks.Many<Order> alpacaSink) {
        this.portfolioRepository = portfolioRepository;
        this.alpacaLive = alpacaLive;
        this.alpacaFlux = alpacaFlux;
        this.alpacaSink = alpacaSink;
    }

    @GetMapping(path = "/alpaca/triggerBuy", produces = "application/json")
    public @ResponseBody
    Double triggerBuy() {
        LOG.info("Triggering manual buy/bid.");
        CONFIG.setTriggerBuy(true);
        return (double) 0;
    }

    @GetMapping(path = "/alpaca/triggerSell", produces = "application/json")
    public @ResponseBody
    Double triggerSell() {
        LOG.info("Triggering manual sell/ask.");
        CONFIG.setTriggerSell(true);
        return (double) 0;
    }

    @GetMapping(path = "/alpaca/portfolio", produces = "application/json")
    public @ResponseBody
    String reportAlpaca() {
        return GSON.toJson(alpacaLive.getPortfolio());
    }

    @GetMapping(path = "/alpaca/currencies", produces = "application/json")
    public @ResponseBody
    String currencies() {
        return GSON.toJson(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc().getCurrencies());
    }

    @GetMapping(value = "/alpaca/streamingOrders", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Order> getAlpacaOrdersStream(){
        return alpacaFlux;
    }

    @PostMapping("/alpaca/order")
    public ResponseEntity<String> saveOrder(@RequestBody String orderString) {
        JsonObject jsonOrder = JsonParser.parseString(orderString).getAsJsonObject();
        LOG.info(jsonOrder);

        final JsonElement type = jsonOrder.get("type");
        final JsonElement assetClass = jsonOrder.get("asset_class");
        final JsonElement symbol = jsonOrder.get("symbol");
        final JsonElement qty = jsonOrder.get("qty");
        final JsonElement side = jsonOrder.get("side");
        final JsonElement limitPrice = jsonOrder.get("limit_price");
        if (type.isJsonNull() || assetClass.isJsonNull() || symbol.isJsonNull() || qty.isJsonNull() || side.isJsonNull() || limitPrice.isJsonNull()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(null);
        }

        final JsonElement filledQty = jsonOrder.get("filled_qty");
        final JsonElement status = jsonOrder.get("status");
        final JsonElement filledAvgPrice = jsonOrder.get("filled_avg_price");

        Order order = new Order();
        order.setType(OrderType.fromValue(type.getAsString()));
        order.setAssetClass(assetClass.getAsString());
        order.setSymbol(symbol.getAsString());
        order.setSide(OrderSide.fromValue(side.getAsString()));
        order.setId(String.valueOf(new Date().getTime()));
        order.setLimitPrice(limitPrice.getAsString());
        order.setQuantity(qty.getAsString());
        order.setFilledQuantity(filledQty.getAsString());
        order.setSubmittedAt(ZonedDateTime.now());
        order.setStatus(OrderStatus.fromValue(status.getAsString()));
        order.setAverageFillPrice(filledAvgPrice.getAsString());
        fillTestOrders(order);

        return ResponseEntity.ok(order.getId());
    }

    void fillTestOrders(Order order) {
        new Timer("Receive Timer").schedule(new TimerTask() {
            @Override
            public void run() {
                alpacaSink.tryEmitNext(order);
            }
        }, 250L);

        new Timer("Fill Timer").schedule(new TimerTask() {
            @Override
            public void run() {
                order.setStatus(OrderStatus.FILLED);
                order.setAverageFillPrice("1");
                order.setFilledQuantity(order.getQuantity());

                alpacaSink.tryEmitNext(order);
            }
        }, 750L);
    }}
