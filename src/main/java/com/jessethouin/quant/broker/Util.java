package com.jessethouin.quant.broker;

import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.beans.*;
import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyType;
import com.jessethouin.quant.conf.MAType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

@Component
public class Util {
    private static final Logger LOG = LogManager.getLogger(Util.class);
    static BacktestParameterCombos backtestParameterCombos;

    public Util(BacktestParameterCombos backtestParameterCombos) {
        Util.backtestParameterCombos = backtestParameterCombos;
    }

    public static BigDecimal getPortfolioValue(Portfolio portfolio, Currency currency, BigDecimal price) {
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        holdings.updateAndGet(v -> v.add(currency.getQuantity()));
        portfolio.getSecurities().stream()
                .filter(security -> security.getCurrency().equals(currency))
                .forEach(security ->
                        holdings.updateAndGet(v -> v.add(security.getSecurityPosition().getQuantity().multiply(security.getSecurityPosition().getPrice()))));
        return holdings.get();
    }

    public static BigDecimal getValueAtPrice(Currency base, BigDecimal marketPrice) {
        return base.getQuantity().multiply(marketPrice).setScale(8, RoundingMode.HALF_UP);
    }

    public static BigDecimal getValueAtPrice(Security security, BigDecimal marketPrice) {
        return security.getSecurityPosition().getQuantity().multiply(marketPrice).setScale(8, RoundingMode.HALF_UP);
    }

    public static BigDecimal getPurchaseBudget(BigDecimal price, BigDecimal allowance, Currency base, Security security) {
        int scale = security == null ? 4 : 0;
        return base.getQuantity().multiply(allowance).divide(price, scale, RoundingMode.FLOOR);
    }

    public static BigDecimal getMA(BigDecimal previousMA, int lookback, BigDecimal price) {
        if (previousMA == null || previousMA.compareTo(BigDecimal.ZERO) == 0 || previousMA.compareTo(BigDecimal.valueOf(0)) == 0)
            previousMA = price;
        return MA.ma(price, previousMA, lookback, MAType.DEMA);
    }

    /**
     * If a Security with the symbol exists in the portfolio, returns that Security, otherwise creates a new Security,
     * adds to portfolio, and returns the new Security.
     *
     * @param symbol    Stock symbol
     * @param portfolio Active portfolio
     * @return A Security object, either existing or new
     */
    public static Security getSecurityFromPortfolio(String symbol, Portfolio portfolio) {
        Optional<Security> s = portfolio.getSecurities().stream().filter(security -> security.getSymbol().equals(symbol)).findFirst();

        if (s.isPresent()) {
            return s.get();
        } else {
            Security security = new Security();
            security.setSymbol(symbol);
            security.setPortfolio(portfolio);
            security.setCurrency(getCurrencyFromPortfolio("USD", portfolio, CurrencyType.FIAT)); // todo: find a dynamic way to get currency of a security.
            SecurityPosition securityPosition = security.getSecurityPosition();
            securityPosition.setSecurity(security);
            securityPosition.setOpened(new Date());
            securityPosition.setPrice(BigDecimal.ZERO);
            securityPosition.setQuantity(BigDecimal.ZERO);
            portfolio.getSecurities().add(security);
            return security;
        }
    }

    /**
     * If a Currency with the symbol exists in the portfolio, returns that Currency, otherwise creates a new Currency,
     * adds to portfolio, and returns the new Currency.
     *
     * @param symbol    Currency symbol
     * @param portfolio Active portfolio
     * @return A Currency object, either existing or new
     */
    public static Currency getCurrencyFromPortfolio(String symbol, Portfolio portfolio) {
        switch (CONFIG.getBroker()) {
            case COINBASE, CEXIO, BINANCE, BINANCE_TEST -> {
                return getCurrencyFromPortfolio(symbol, portfolio, CurrencyType.CRYPTO);
            }
            default -> {
                return getCurrencyFromPortfolio(symbol, portfolio, CurrencyType.FIAT);
            }
        }
    }

    public static Currency getCurrencyFromPortfolio(String symbol, Portfolio portfolio, CurrencyType currencyType) {
        Optional<Currency> c = portfolio.getCurrencies().stream().filter(currency -> currency.getSymbol().equals(symbol)).findFirst();

        if (c.isPresent()) {
            return c.get();
        } else {
            Currency currency = new Currency();
            currency.setCurrencyType(currencyType);
            currency.setSymbol(symbol);
            currency.setQuantity(BigDecimal.ZERO);
            currency.setPortfolio(portfolio);
            portfolio.getCurrencies().add(currency);
            return currency;
        }
    }

    public static Portfolio createEmptyPortfolio() {
        Portfolio portfolio = new Portfolio();
        Currency currency = new Currency();
        currency.setSymbol("USD");
        currency.setQuantity(BigDecimal.ZERO);
        currency.setCurrencyType(CurrencyType.FIAT);
        currency.setPortfolio(portfolio);
        portfolio.getCurrencies().add(currency);
        return portfolio;
    }

    public static Portfolio createPortfolio() {
        Portfolio portfolio = new Portfolio();

        List<String> fiatCurrencies = CONFIG.getFiatCurrencies();
        fiatCurrencies.forEach(c -> {
            Currency currency = new Currency();
            currency.setSymbol(c);
            currency.setQuantity(BigDecimal.ZERO);
            currency.setCurrencyType(CurrencyType.FIAT);
            if (c.equals("USD")) { // default-coded (you're welcome, Pra) for now, until international exchanges are implemented in Alpaca. In other words, ALL securities traded are in USD.
                Util.credit(currency, CONFIG.getInitialCash(), "Initializing portfolio with default config values", null);
                List<String> tickers = CONFIG.getSecurities();
                tickers.forEach(t -> {
                    Security security = new Security();
                    security.setSymbol(t);
                    security.setCurrency(currency);
                    security.setPortfolio(portfolio);
                    portfolio.getSecurities().add(security);
                });
            }
            currency.setPortfolio(portfolio);
            portfolio.getCurrencies().add(currency);
        });

        List<String> cryptoCurrencies = CONFIG.getCryptoCurrencies();
        cryptoCurrencies.forEach(c -> {
            Currency currency = new Currency();
            currency.setSymbol(c);
            currency.setQuantity(BigDecimal.ZERO);
            currency.setCurrencyType(CurrencyType.CRYPTO);
            currency.setPortfolio(portfolio);
            portfolio.getCurrencies().add(currency);
        });

        Currency usdt = getCurrencyFromPortfolio("USDT", portfolio, CurrencyType.CRYPTO);
        Util.credit(usdt, CONFIG.getInitialCash(), "Initializing portfolio with default config values", null);

        return portfolio;
    }

    public static String formatFiat(Object o) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setMaximumFractionDigits(4);
        return numberFormat.format(o);
    }

    public static synchronized void debit(Currency currency, BigDecimal qty, String memo, String orderId) {
        // todo: implement overdraft protection/exception
        CurrencyLedger currencyLedger = CurrencyLedger.builder().currency(currency).debit(qty).timestamp(new Date()).memo(memo).orderId(orderId).build();
        currency.getCurrencyLedgers().add(currencyLedger);
        currency.setQuantity(currency.getQuantity().subtract(requireNonNullElse(qty, BigDecimal.ZERO)));
    }

    public static synchronized void credit(Currency currency, BigDecimal qty, String memo, String orderId) {
        // todo: implement overdraft protection/exception
        CurrencyLedger currencyLedger = CurrencyLedger.builder().currency(currency).credit(qty).timestamp(new Date()).memo(memo).orderId(orderId).build();
        currency.getCurrencyLedgers().add(currencyLedger);
        currency.setQuantity(currency.getQuantity().add(requireNonNullElse(qty, BigDecimal.ZERO)));
    }

    public static void recalibrate(Config config, boolean updateBacktest) {
        if (updateBacktest) CONFIG.setBackTest(true);
        BacktestParameterResults bestCombo = backtestParameterCombos.findBestCombo();
        config.setLowRisk(bestCombo.getLowRisk());
        config.setHighRisk(bestCombo.getHighRisk());
        config.setShortLookback(bestCombo.getShortLookback());
        config.setLongLookback(bestCombo.getLongLookback());
        config.setAllowance(bestCombo.getAllowance());
        config.setBuyStrategy(bestCombo.getBuyStrategyType());
        config.setSellStrategy(bestCombo.getSellStrategyType());
        config.setBacktestStart(new Date(config.getBacktestStart().getTime() + Duration.ofMinutes(config.getRecalibrateFreq()).toMillis()));
        config.setBacktestEnd(new Date(config.getBacktestEnd().getTime() + Duration.ofMinutes(config.getRecalibrateFreq()).toMillis()));
        if (updateBacktest) CONFIG.setBackTest(false);
    }

}
