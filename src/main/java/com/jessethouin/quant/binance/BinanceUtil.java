package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.CurrencyType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.exceptions.CurrencyPairNotValidException;
import org.knowm.xchange.instrument.Instrument;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.*;
import static com.jessethouin.quant.conf.Config.CONFIG;

public class BinanceUtil {
    private static final Logger LOG = LogManager.getLogger(BinanceUtil.class);

    public static BigDecimal getBreakEven(BigDecimal counterQty, String base, String counter, String tether) {
        BigDecimal rate = BigDecimal.valueOf(0.0750 / 100);

        BigDecimal base_counter = getTickerPrice(base, counter);
        BigDecimal base_tether = getTickerPrice(base, tether);
        BigDecimal counter_tether = getTickerPrice(counter, tether);

        BigDecimal feesInBase = ((rate.multiply(counterQty)).divide(base_counter, 8, RoundingMode.HALF_UP));
        BigDecimal breakEven = ((counter_tether.multiply(rate)).multiply(BigDecimal.valueOf(2))).add(counter_tether);

        LOG.debug("Bought {} {} at {} {}}, costing {}", counterQty, counter, counter_tether, tether, counterQty.multiply(counter_tether));
        LOG.debug("Fees in {}: {} {}", base, feesInBase, base);
        LOG.debug("Fees in {}: ${}", tether, feesInBase.multiply(base_tether));
        LOG.debug("Break even in {}: {}", tether, breakEven);
        return breakEven;
    }

    /*
     * https://github.com/jaggedsoft/php-binance-api/issues/205
     *
     * minQty is the minimum amount you can order (quantity)
     * minNotional is the minimum value of your order. (price * quantity)
     *
     * */
    public static BigDecimal getMinTrade(CurrencyPair currencyPair) {
        BigDecimal[] minTrade = {BigDecimal.ZERO};
        BINANCE_EXCHANGE_INFO.getSymbols().stream()
                .filter(symbol -> symbol.getBaseAsset().equals(currencyPair.base.getSymbol()) && symbol.getQuoteAsset().equals(currencyPair.counter.getSymbol()))
                .forEach(symbol -> Arrays.stream(symbol.getFilters())
                        .filter(filter -> filter.getFilterType().equals("MIN_NOTIONAL"))
                        .forEach(filter -> minTrade[0] = new BigDecimal(filter.getMinNotional())));
        return minTrade[0];
    }

    public static BigDecimal getTickerPrice(String base, String counter) {
        BigDecimal ret = null;
        try {
            ret = BINANCE_MARKET_DATA_SERVICE.getTicker(new CurrencyPair(base, counter)).getLast();
        } catch (IOException e) {
            LOG.error(e.getMessage());
        } catch (CurrencyPairNotValidException e) {
            LOG.info("Currency Pair {}/{} is not valid. Swapping.", base, counter);
        }

        if (ret == null) {
            try {
                ret = BINANCE_MARKET_DATA_SERVICE.getTicker(new CurrencyPair(counter, base)).getLast();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            } catch (CurrencyPairNotValidException e) {
                LOG.info("Currency Pair {}/{} is not valid even after swapping. Return value is NULL, sorry.", counter, base);
            }
        }
        return ret;
    }

    public static void showWallets() {
        try {
            StringBuilder sb = new StringBuilder();
            BINANCE_ACCOUNT_SERVICE.getAccountInfo().getWallets().forEach((_, w) -> {
                w.getBalances().forEach((c, b) -> {
                    if (b.getTotal().compareTo(BigDecimal.ZERO) > 0) {
                        sb.append(System.lineSeparator());
                        sb.append("\t");
                        sb.append(c.getSymbol());
                        sb.append(" - ");
                        sb.append(b.getTotal().toPlainString());
                    }
                });
                LOG.info("Wallet: {}", sb);
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void showTradingFees(Portfolio portfolio) {
        try {
            Map<Instrument, Fee> dynamicTradingFees = BINANCE_ACCOUNT_SERVICE.getDynamicTradingFeesByInstrument();
            dynamicTradingFees.forEach((instrument, fee) -> {
                if (portfolio.getCurrencies().stream().anyMatch(currency -> instrument.getBase().getSymbol().equals(currency.getSymbol())) &&
                        portfolio.getCurrencies().stream().anyMatch(currency -> instrument.getCounter().getSymbol().equals(currency.getSymbol()))) {
                    LOG.info(instrument + " - m : " + fee.getMakerFee() + " t : " + fee.getTakerFee());
                }
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void reconcile(Portfolio portfolio) {
        try {
            BINANCE_EXCHANGE.getTradeService().getOpenOrders().getOpenOrders().forEach(BinanceStreamProcessor::processRemoteOrder);

            BINANCE_EXCHANGE.getAccountService().getAccountInfo().getWallets().forEach((_, wallet) -> wallet.getBalances().forEach((remoteCurrency, balance) -> {
                Currency currency = Util.getCurrencyFromPortfolio(remoteCurrency.getSymbol(), portfolio);
                int diff = currency.getQuantity().compareTo(balance.getAvailable());
                if (diff == 0) {
                    return;
                }
                currency.setCurrencyType(CurrencyType.CRYPTO);
                currency.setPortfolio(portfolio);
                portfolio.getCurrencies().add(currency);
                LOG.info("{}: Reconciling local ledger ({}) with remote wallet ({}).", currency.getSymbol(), currency.getQuantity(), balance.getAvailable());
                if (diff > 0) {
                    Util.debit(currency, currency.getQuantity().subtract(balance.getAvailable()).abs(), "Reconciling with Binance wallet", null);
                }
                if (diff < 0) {
                    Util.credit(currency, currency.getQuantity().add(balance.getAvailable()).abs(), "Reconciling with Binance wallet", null);
                }
            }));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static List<CurrencyPair> getAllCryptoCurrencyPairs() {
        List<CurrencyPair> currencyPairs = new ArrayList<>();
        CONFIG.getCryptoCurrencies().forEach(base -> CONFIG.getCryptoCurrencies().forEach(counter -> {
            if (!base.equals(counter) && !currencyPairs.contains(new CurrencyPair(counter, base))) {
                BINANCE_EXCHANGE.getExchangeMetaData().getInstruments()
                        .keySet()
                        .stream()
                        .filter(instrument -> instrument.getBase().getSymbol().equals(base) && instrument.getCounter().getSymbol().equals(counter))
                        .forEach(_ -> currencyPairs.add(new CurrencyPair(counter, base)));
            }
        }));
        return currencyPairs;
    }
}
