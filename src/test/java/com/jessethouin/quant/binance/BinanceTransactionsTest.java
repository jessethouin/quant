package com.jessethouin.quant.binance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.binance.service.BinanceTradeService;
import org.knowm.xchange.currency.CurrencyPair;

import java.io.IOException;
import java.math.BigDecimal;

class BinanceTransactionsTest {
    static Exchange exchange;
    static BinanceTradeService tradeService;

    @BeforeEach
    void setUp() {
        exchange = ExchangeFactory.INSTANCE.createExchange(BinanceExchange.class);
        tradeService = (BinanceTradeService) exchange.getTradeService();
    }

    @Test
    void buySecurity() throws IOException {
        BinanceTransactions.buyCurrency(CurrencyPair.BTC_USDT, BigDecimal.ONE, limitPriceForCurrencyPair(CurrencyPair.BTC_USDT));
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