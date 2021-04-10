package com.jessethouin.quant;

import static com.jessethouin.quant.conf.Config.CONFIG;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class QuantController {
    private static final Logger LOG = LogManager.getLogger(QuantController.class);

    @GetMapping(path="/triggerBuy", produces = "application/json")
    public @ResponseBody
    Double triggerBuy() {
        LOG.info("Triggering manual buy/bid.");
        CONFIG.setTriggerBuy(true);
        return (double) 0;
    }

    @GetMapping(path="/triggerSell", produces = "application/json")
    public @ResponseBody
    Double triggerSell() {
        LOG.info("Triggering manual sell/ask.");
        CONFIG.setTriggerSell(true);
        return (double) 0;
    }

    @GetMapping(path="/report", produces = "application/json")
    public @ResponseBody
    Double report() {
        return (double) 0;
    }
}
