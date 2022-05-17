package com.jessethouin.quant.broker;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.beans.Security;
import com.jessethouin.quant.calculators.Calc;
import com.jessethouin.quant.conf.Instrument;
import lombok.Getter;
import lombok.Setter;
import org.knowm.xchange.currency.CurrencyPair;

import java.math.BigDecimal;
import java.util.Date;

import static com.jessethouin.quant.conf.Config.CONFIG;

@Getter
@Setter
public class Fundamental {
    Instrument instrument;
    CurrencyPair currencyPair;
    Calc calc;
    Security security;
    Currency baseCurrency;
    Currency counterCurrency;
    BigDecimal shortMAValue;
    BigDecimal longMAValue;
    BigDecimal price = BigDecimal.ZERO;
    BigDecimal previousShortMAValue = BigDecimal.ZERO;
    BigDecimal previousLongMAValue = BigDecimal.ZERO;
    BigDecimal value = BigDecimal.ZERO;
    BigDecimal previousValue = BigDecimal.ZERO;
    Date timestamp;
    int count = 0;

    public Fundamental(Instrument instrument, CurrencyPair currencyPair, Portfolio portfolio) {
        this.instrument = instrument;
        this.currencyPair = currencyPair;
        this.baseCurrency = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        this.counterCurrency = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
        calc = new Calc(baseCurrency, counterCurrency, CONFIG, BigDecimal.ZERO);
    }

    public Fundamental(Instrument instrument, Currency base, Currency counter) {
        this.instrument = instrument;
        this.baseCurrency = base;
        this.counterCurrency = counter;
        calc = new Calc(base, counter, CONFIG, BigDecimal.ZERO);
    }

    public Fundamental(Instrument instrument, Security security) {
        this.instrument = instrument;
        this.security = security;
        this.baseCurrency = security.getCurrency();
        calc = new Calc(security, CONFIG, BigDecimal.ZERO);
    }

    public void update(Portfolio portfolio) {
        if (instrument.equals(Instrument.STOCK)) setSecurity(Util.getSecurityFromPortfolio(getSecurity().getSymbol(), portfolio));
        setBaseCurrency(Util.getCurrencyFromPortfolio(getBaseCurrency().getSymbol(), portfolio, getBaseCurrency().getCurrencyType()));
        if (instrument.equals(Instrument.CRYPTO) || instrument.equals(Instrument.FIAT)) setCounterCurrency(Util.getCurrencyFromPortfolio(getCounterCurrency().getSymbol(), portfolio, getCounterCurrency().getCurrencyType()));
        getCalc().setSecurity(getSecurity());
        getCalc().setBase(getBaseCurrency());
        getCalc().setCounter(getCounterCurrency());
    }
}
