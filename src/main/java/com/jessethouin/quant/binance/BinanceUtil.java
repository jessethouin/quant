package com.jessethouin.quant.binance;

import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Filter;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Symbol;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.CurrencyPairNotValidException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_EXCHANGE_INFO;
import static com.jessethouin.quant.binance.BinanceExchangeServices.BINANCE_MARKET_DATA_SERVICE;

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
    
    public static void updateBinanceLimitOrder(BinanceLimitOrder binanceLimitOrder, LimitOrder limitOrder) {
        binanceLimitOrder.setType(limitOrder.getType());
        binanceLimitOrder.setOriginalAmount(limitOrder.getOriginalAmount());
        binanceLimitOrder.setInstrument(limitOrder.getInstrument().toString());
        binanceLimitOrder.setId(limitOrder.getId());
        binanceLimitOrder.setTimestamp(limitOrder.getTimestamp());
        binanceLimitOrder.setLimitPrice(limitOrder.getLimitPrice());
        binanceLimitOrder.setAveragePrice(limitOrder.getAveragePrice());
        binanceLimitOrder.setCumulativeAmount(limitOrder.getCumulativeAmount());
        binanceLimitOrder.setFee(limitOrder.getFee());
        binanceLimitOrder.setStatus(limitOrder.getStatus());
        binanceLimitOrder.setUserReference(limitOrder.getUserReference());
        binanceLimitOrder.setCumulativeAmount(limitOrder.getCumulativeCounterAmount());
        binanceLimitOrder.setLeverage(limitOrder.getLeverage());
    }
}
