package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Security;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.service.account.AccountService;

import java.io.IOException;
import java.math.BigDecimal;

public class BinanceTransactions {
    private static final Logger LOG = LogManager.getLogger(BinanceTransactions.class);

    public static void buySecurity(Security security, BigDecimal qty, BigDecimal price) {
        ExchangeSpecification exSpec = new BinanceExchange().getDefaultExchangeSpecification();
        exSpec.setUserName("54704697");
        exSpec.setApiKey("M4gIEsmhsp5MjIkSZRapUUxScnZno56OHwJOvh1Bp3qIxW54FGCZnOxUYneNjVXB");
        exSpec.setSecretKey("tlAe5qFbA8oVDH0M085pYANzRD0EPHVteicsKk6rlKg1gEdC3j1lkSF3FMpd7jkO");

        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(exSpec);

        AccountService accountService = exchange.getAccountService();
        try {
            StringBuilder sb = new StringBuilder("Wallet: ");
            accountService.getAccountInfo().getWallets().forEach((s,w) -> {
                sb.append(s);
                w.getBalances().forEach((c, b) -> {
                    sb.append(c.getSymbol());
                    sb.append(" - ");
                    sb.append(b.getTotal());
                });
                LOG.info("Wallet: {} - {}", s, sb);
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}