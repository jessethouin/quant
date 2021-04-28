package com.jessethouin.quant.broker;

import static com.jessethouin.quant.conf.Config.CONFIG;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.calculators.Calc;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.knowm.xchange.currency.CurrencyPair;

@Getter
@Setter
public class Fundamentals {
    CurrencyPair currencyPair;
    Calc calc;
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

    public Fundamentals(CurrencyPair currencyPair, Portfolio portfolio) {
        this.currencyPair = currencyPair;
        this.baseCurrency = Util.getCurrencyFromPortfolio(currencyPair.base.getSymbol(), portfolio);
        this.counterCurrency = Util.getCurrencyFromPortfolio(currencyPair.counter.getSymbol(), portfolio);
        calc = new Calc(baseCurrency, counterCurrency, CONFIG, BigDecimal.ZERO);
    }

    public void update(Portfolio portfolio) {
        setBaseCurrency(Util.getCurrencyFromPortfolio(getBaseCurrency().getSymbol(), portfolio));
        setCounterCurrency(Util.getCurrencyFromPortfolio(getCounterCurrency().getSymbol(), portfolio));
        getCalc().setBase(getBaseCurrency());
        getCalc().setCounter(getCounterCurrency());
    }
}
