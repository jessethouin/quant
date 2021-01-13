package com.jessethouin.quant.binance;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

class BinanceTransactionsTest {
    static Exchange exchange;
    static BinanceTradeService tradeService;

    @BeforeEach
    void setUp() {
        exchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange.class);
        tradeService = (BinanceTradeService) exchange.getTradeService();
    }

    @Test
    void testRounding() {
        BigDecimal a = new BigDecimal("0.783009375");
        BigDecimal b = a.multiply(BigDecimal.ONE).setScale(8, RoundingMode.FLOOR);
        Assertions.assertEquals("0.78300937", b.toPlainString());

        BigDecimal c = new BigDecimal("0.000000001");
        BigDecimal d = c.multiply(BigDecimal.ONE).setScale(8, RoundingMode.FLOOR);
        Assertions.assertEquals("0.00000000", d.toPlainString());

        System.out.println("b: " + b.toPlainString());
        System.out.println("d: " + d.toPlainString());
    }

    private BigDecimal limitPriceForCurrencyPair(CurrencyPair currencyPair) throws IOException {
        return exchange
                .getMarketDataService()
                .getOrderBook(currencyPair)
                .getAsks()
                .get(0)
                .getLimitPrice();
    }
}