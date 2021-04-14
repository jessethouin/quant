package com.jessethouin.quant;

import static com.jessethouin.quant.conf.Config.CONFIG;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.jessethouin.quant.beans.repos.PortfolioRepository;
import com.jessethouin.quant.binance.BinanceLive;
import com.jessethouin.quant.db.Exclude;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class QuantController {

    private static final Logger LOG = LogManager.getLogger(QuantController.class);
    private final PortfolioRepository portfolioRepository;
    private final BinanceLive binanceLive;

    public QuantController(PortfolioRepository portfolioRepository, BinanceLive binanceLive) {
        this.portfolioRepository = portfolioRepository;
        this.binanceLive = binanceLive;
    }

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
        GSON = new GsonBuilder()
            .addSerializationExclusionStrategy(EXCLUSION_STRATEGY)
            .registerTypeAdapter(ZonedDateTime.class, (JsonDeserializer<ZonedDateTime>) (json, type, jsonDeserializationContext) -> ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()))
            .create();
    }

    @GetMapping(path = "/triggerBuy", produces = "application/json")
    public @ResponseBody
    Double triggerBuy() {
        LOG.info("Triggering manual buy/bid.");
        CONFIG.setTriggerBuy(true);
        return (double) 0;
    }

    @GetMapping(path = "/triggerSell", produces = "application/json")
    public @ResponseBody
    Double triggerSell() {
        LOG.info("Triggering manual sell/ask.");
        CONFIG.setTriggerSell(true);
        return (double) 0;
    }

    @GetMapping(path = "/portfolio", produces = "application/json")
    public @ResponseBody
    String report() {
        // return GSON.toJson(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc());
        return GSON.toJson(binanceLive.getPortfolio());
    }

    @GetMapping(path = "/currencies", produces = "application/json")
    public @ResponseBody
    String currencies() {
        return GSON.toJson(portfolioRepository.getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc().getCurrencies());
    }
}
