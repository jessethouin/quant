package com.jessethouin.quant.broker;

import com.jessethouin.quant.binance.BinanceUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;

class UtilTest {
    private static final Logger LOG = LogManager.getLogger(UtilTest.class);

    @Test
    void getTickerPrice() {
        BigDecimal validPair = BinanceUtil.getTickerPrice("ETH", "BTC");
        System.out.println(validPair);
        Assertions.assertNotNull(validPair);
        Assertions.assertTrue(validPair.compareTo(BigDecimal.ZERO) > 0);

        BigDecimal invalidPair = BinanceUtil.getTickerPrice("BTC", "ETH");
        System.out.println(invalidPair);
        Assertions.assertNotNull(invalidPair);
        Assertions.assertTrue(invalidPair.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getBreakEven() {
        BigDecimal breakEven = BinanceUtil.getBreakEven(BigDecimal.valueOf(0.000584), "BNB", "BTC", "USDT");
        LOG.info("Break even: {}", breakEven);
        Assertions.assertTrue(breakEven.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getMinTrade() {
        BigDecimal minTrade = BinanceUtil.getMinTrade(CurrencyPair.BTC_USDT);
        LOG.info("Min Trade: {}", minTrade);
        Assertions.assertTrue(minTrade.compareTo(BigDecimal.ZERO) > 0);
    }
}