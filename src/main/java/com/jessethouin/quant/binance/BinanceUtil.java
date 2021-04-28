package com.jessethouin.quant.binance;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_ACCOUNT_SERVICE;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_EXCHANGE;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_EXCHANGE_INFO;
import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_MARKET_DATA_SERVICE;

import com.jessethouin.quant.beans.Currency;
import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.broker.Util;
import com.jessethouin.quant.conf.Config;
import com.jessethouin.quant.conf.CurrencyTypes;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Filter;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Symbol;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.exceptions.CurrencyPairNotValidException;

public class BinanceUtil {
    private static final Logger LOG = LogManager.getLogger(BinanceUtil.class);

    public static BigDecimal getBreakEven(BigDecimal qtyBTC) {
        BigDecimal rate = BigDecimal.valueOf(0.0750 / 100);

        BigDecimal BNB_BTC = getTickerPrice("BNB", "BTC"); // needs to be dynamic
        BigDecimal BNB_USDT = getTickerPrice("BNB", "USDT"); // needs to be dynamic
        BigDecimal BTC_USDT = getTickerPrice("BTC", "USDT"); // needs to be dynamic, based off of USDC

        BigDecimal feesInBNB = ((rate.multiply(qtyBTC)).divide(BNB_BTC, 8, RoundingMode.HALF_UP));
        BigDecimal breakEven = ((BTC_USDT.multiply(rate)).multiply(BigDecimal.valueOf(2))).add(BTC_USDT);

        LOG.debug("Bought {} BTC at {} USDT, costing {}", qtyBTC, BTC_USDT, qtyBTC.multiply(BTC_USDT));
        LOG.debug("Fees in BNB: {} BNB", feesInBNB);
        LOG.debug("Fees in USDT: ${}", feesInBNB.multiply(BNB_USDT));
        LOG.debug("Break even in USDT: {}", breakEven);
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
        Symbol[] symbols = BINANCE_EXCHANGE_INFO.getSymbols();
        List<Symbol> symbolList = Arrays.stream(symbols).filter(symbol -> symbol.getBaseAsset().equals(currencyPair.base.getSymbol()) && symbol.getQuoteAsset().equals(currencyPair.counter.getSymbol())).collect(Collectors.toList());
        symbolList.forEach(symbol -> {
            List<Filter> filters = Arrays.stream(symbol.getFilters()).filter(filter -> filter.getFilterType().equals("MIN_NOTIONAL")).collect(Collectors.toList());
            filters.forEach(filter -> minTrade[0] = new BigDecimal(filter.getMinNotional()));
        });
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

    public static List<CurrencyPair> getAllCryptoCurrencyPairs(Config config) {
        List<CurrencyPair> currencyPairs = new ArrayList<>();
        config.getCryptoCurrencies().forEach(base -> config.getCryptoCurrencies().forEach(counter -> {
            if (!base.equals(counter) && !currencyPairs.contains(new CurrencyPair(counter, base))) {
                BINANCE_EXCHANGE.getExchangeSymbols().stream().filter(currencyPair -> currencyPair.base.getSymbol().equals(base) && currencyPair.counter.getSymbol().equals(counter)).forEach(currencyPairs::add);
            }
        }));
        return currencyPairs;
    }

    public static void showWallets() {
        try {
            StringBuilder sb = new StringBuilder();
            BINANCE_ACCOUNT_SERVICE.getAccountInfo().getWallets().forEach((s, w) -> {
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
            Map<CurrencyPair, Fee> dynamicTradingFees = BINANCE_ACCOUNT_SERVICE.getDynamicTradingFees();
            dynamicTradingFees.forEach((c, f) -> {
                if (portfolio.getCurrencies().stream().anyMatch(currency -> c.base.getSymbol().equals(currency.getSymbol())) &&
                        portfolio.getCurrencies().stream().anyMatch(currency -> c.counter.getSymbol().equals(currency.getSymbol()))) {
                    LOG.info(c + " - m : " + f.getMakerFee() + " t : " + f.getTakerFee());
                }
            });
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }

    public static void reconcile(Portfolio portfolio) {
        try {
            BINANCE_EXCHANGE.getTradeService().getOpenOrders().getOpenOrders().forEach(BinanceStreamProcessing::processRemoteOrder);

            BINANCE_EXCHANGE.getAccountService().getAccountInfo().getWallets().forEach((s, wallet) -> wallet.getBalances().forEach((remoteCurrency, balance) -> {
                Currency currency = Util.getCurrencyFromPortfolio(remoteCurrency.getSymbol(), portfolio);
                int diff = currency.getQuantity().compareTo(balance.getAvailable());
                if (diff == 0) {
                    return;
                }
                currency.setCurrencyType(CurrencyTypes.CRYPTO);
                currency.setPortfolio(portfolio);
                portfolio.getCurrencies().add(currency);
                LOG.info("{}: Reconciling local ledger ({}) with remote wallet ({}).", currency.getSymbol(), currency.getQuantity(), balance.getAvailable());
                if (diff > 0) {
                    Util.debit(currency, currency.getQuantity().subtract(balance.getAvailable()).abs(), "Reconciling with Binance wallet");
                }
                if (diff < 0) {
                    Util.credit(currency, currency.getQuantity().add(balance.getAvailable()).abs(), "Reconciling with Binance wallet");
                }
            }));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}
