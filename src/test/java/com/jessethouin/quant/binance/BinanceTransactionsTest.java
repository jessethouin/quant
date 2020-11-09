package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class BinanceTransactionsTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void buySecurity() {
        Security security = new Security();
        security.setSymbol("BTC_USDT");
        BinanceTransactions.buySecurity(security, BigDecimal.ONE, BigDecimal.TEN);
    }
}