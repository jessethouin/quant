package com.jessethouin.quant.broker;

import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.CurrencyLedger;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.calculators.MA;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyTypes;
import com.jessethouin.quant.conf.MATypes;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.jessethouin.quant.conf.Config.CONFIG;
import static java.util.Objects.requireNonNullElse;

public class Util {

    public static BigDecimal getPortfolioValue(Portfolio portfolio, Currency currency, BigDecimal price) {
        AtomicReference<BigDecimal> holdings = new AtomicReference<>(BigDecimal.ZERO);
        holdings.updateAndGet(v -> v.add(currency.getQuantity()));
        portfolio.getSecurities().stream()
                .filter(security -> security.getCurrency().equals(currency))
                .forEach(security -> security.getSecurityPositions()
                        .forEach(position -> holdings.updateAndGet(v -> v.add(price.multiply(position.getQuantity())))));
        return holdings.get();
    }

    public static BigDecimal getValueAtPrice(Currency base, BigDecimal marketPrice) {
        return base.getQuantity().multiply(marketPrice);
    }

    public static List<CurrencyPair> getAllCurrencyPairs(Config config) {
        List<CurrencyPair> currencyPairs = new ArrayList<>();
        config.getCryptoCurrencies().forEach(base -> config.getCryptoCurrencies().forEach(counter -> {
            if (!base.equals(counter) && !currencyPairs.contains(new CurrencyPair(counter, base))) {
                currencyPairs.add(new CurrencyPair(base, counter));
            }
        }));
        return currencyPairs;
    }

    public static BigDecimal getBudget(BigDecimal price, BigDecimal allowance, Currency currency, Security security) {
        int scale = security == null ? 8 : 0;
        return currency.getQuantity().multiply(allowance).divide(price, scale, RoundingMode.FLOOR);
    }

    public static BigDecimal getMA(BigDecimal previousMA, int lookback, BigDecimal price) {
        if (previousMA == null || previousMA.compareTo(BigDecimal.ZERO) == 0 || previousMA.compareTo(BigDecimal.valueOf(0)) == 0) previousMA = price;
        return MA.ma(price, previousMA, lookback, MATypes.TEMA);
    }

    /**
     * If a Security with the symbol exists in the portfolio, returns that Security, otherwise creates a new Security,
     * adds to portfolio, and returnes the new Security.
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
            security.setCurrency(getCurrencyFromPortfolio("USD", portfolio)); // todo: find a dynamic way to get currency of a security.
            return security;
        }
    }

    /**
     * If a Currency with the symbol exists in the portfolio, returns that Currency, otherwise creates a new Currency,
     * adds to portfolio, and returnes the new Currency.
     *
     * @param symbol    Currency symbol
     * @param portfolio Active portfolio
     * @return A Currency object, either existing or new
     */
    public static Currency getCurrencyFromPortfolio(String symbol, Portfolio portfolio) {
        Optional<Currency> c = portfolio.getCurrencies().stream().filter(currency -> currency.getSymbol().equals(symbol)).findFirst();

        if (c.isPresent()) {
            return c.get();
        } else {
            Currency currency = new Currency();
            currency.setCurrencyType(CurrencyTypes.FIAT);
            currency.setSymbol(symbol);
            currency.setQuantity(BigDecimal.ZERO);
            currency.setPortfolio(portfolio);
            return currency;
        }
    }

    public static Portfolio createPortfolio() {
        Portfolio portfolio = new Portfolio();

        List<String> fiatCurrencies = CONFIG.getFiatCurrencies();
        fiatCurrencies.forEach(c -> {
            Currency currency = new Currency();
            currency.setSymbol(c);
            currency.setQuantity(BigDecimal.ZERO);
            currency.setCurrencyType(CurrencyTypes.FIAT);
            if (c.equals("USD")) { // default-coded (you're welcome, Pra) for now, until international exchanges are implemented in Alpaca. In other words, ALL securities traded are in USD.
                Util.credit(currency, CONFIG.getInitialCash());
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
            currency.setCurrencyType(CurrencyTypes.CRYPTO);
            currency.setPortfolio(portfolio);
            portfolio.getCurrencies().add(currency);
        });

        Currency usdt = getCurrencyFromPortfolio("USDT", portfolio);
        Util.credit(usdt, CONFIG.getInitialCash());

        return portfolio;
    }

    public static String formatFiat(Object o) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        numberFormat.setMaximumFractionDigits(4);
        return numberFormat.format(o);
    }

    public static void debit(Currency currency, BigDecimal qty) {
        CurrencyLedger currencyLedger = CurrencyLedger.builder().currency(currency).debit(qty).timestamp(new Date()).build();
        currency.getCurrencyLedgers().add(currencyLedger);
        currency.setQuantity(currency.getQuantity().subtract(requireNonNullElse(qty, BigDecimal.ZERO)));
    }

    public static void credit(Currency currency, BigDecimal qty) {
        CurrencyLedger currencyLedger = CurrencyLedger.builder().currency(currency).credit(qty).timestamp(new Date()).build();
        currency.getCurrencyLedgers().add(currencyLedger);
        currency.setQuantity(currency.getQuantity().add(requireNonNullElse(qty, BigDecimal.ZERO)));
    }

    public static void relacibrate(Config config) {
        BacktestParameterResults bestCombo = BacktestParameterCombos.findBestCombo();
        config.setLowRisk(bestCombo.getLowRisk());
        config.setHighRisk(bestCombo.getHighRisk());
        config.setShortLookback(bestCombo.getShortLookback());
        config.setLongLookback(bestCombo.getLongLookback());
        config.setAllowance(bestCombo.getAllowance());
        config.setBuyStrategy(bestCombo.getBuyStrategyType());
        config.setSellStrategy(bestCombo.getSellStrategyType());
        config.setBacktestStart(new Date(config.getBacktestStart().getTime() + Duration.ofMinutes(config.getRecalibrateFreq()).toMillis()));
        config.setBacktestEnd(new Date(config.getBacktestEnd().getTime() + Duration.ofMinutes(config.getRecalibrateFreq()).toMillis()));
    }
}
