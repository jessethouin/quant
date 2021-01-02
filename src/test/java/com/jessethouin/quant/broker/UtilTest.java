package com.jessethouin.quant.broker;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.CurrencyPosition;
import com.jessethouin.quant.conf.CurrencyTypes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Date;

class UtilTest {
    private static final Logger LOG = LogManager.getLogger(UtilTest.class);

    @Test
    void getCurrencyPositionValue() {
        Currency btc = new Currency();
        btc.setCurrencyType(CurrencyTypes.CRYPTO);
        btc.setSymbol("BTC");

        Currency eth = new Currency();
        eth.setCurrencyType(CurrencyTypes.CRYPTO);
        eth.setSymbol("ETH");

        Currency usdt = new Currency();
        usdt.setCurrencyType(CurrencyTypes.CRYPTO);
        usdt.setSymbol("USDT");

        CurrencyPosition currencyPosition  = new CurrencyPosition();
        currencyPosition.setQuantity(BigDecimal.TEN);
        currencyPosition.setOpened(new Date());
        currencyPosition.setBaseCurrency(btc);
        currencyPosition.setCounterCurrency(eth);
        currencyPosition.setPrice(BigDecimal.valueOf(0.040));

        btc.getCurrencyPositions().add(currencyPosition);

        BigDecimal currencyPositionValue = Util.getCurrencyPositionValue(currencyPosition, usdt);

        assert currencyPositionValue != null && currencyPositionValue.compareTo(BigDecimal.ZERO) > 0;
    }

    @Test
    void getTickerPrice() {
        BigDecimal validPair = Util.getTickerPrice("ETH", "BTC");
        System.out.println(validPair);
        Assertions.assertNotNull(validPair);
        Assertions.assertTrue(validPair.compareTo(BigDecimal.ZERO) > 0);

        BigDecimal invalidPair = Util.getTickerPrice("BTC", "ETH");
        System.out.println(invalidPair);
        Assertions.assertNotNull(invalidPair);
        Assertions.assertTrue(invalidPair.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void getBreakEven() {
        BigDecimal breakEven = Util.getBreakEven(BigDecimal.valueOf(0.000584));
        LOG.info("Break even: {}", breakEven);
        Assertions.assertTrue(breakEven.compareTo(BigDecimal.ZERO) > 0);
    }
}