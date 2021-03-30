package com.jessethouin.quant.binance;

import com.jessethouin.quant.beans.Portfolio;
import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import com.jessethouin.quant.binance.beans.repos.BinanceLimitOrderRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Filter;
import org.knowm.xchange.binance.dto.meta.exchangeinfo.Symbol;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.exceptions.CurrencyPairNotValidException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BinanceUtil {
    private static final Logger LOG = LogManager.getLogger(BinanceUtil.class);
    private static BinanceLive binanceLive;
    private static BinanceLimitOrderRepository binanceLimitOrderRepository;

    public BinanceUtil(BinanceLive binanceLive, BinanceLimitOrderRepository binanceLimitOrderRepository) {
        BinanceUtil.binanceLive = binanceLive;
        BinanceUtil.binanceLimitOrderRepository = binanceLimitOrderRepository;
    }

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

    public static BigDecimal getMinTrade(CurrencyPair currencyPair) {
        BigDecimal[] minTrade = {BigDecimal.ZERO};
        Symbol[] symbols = binanceLive.getBinanceExchangeInfo().getSymbols();
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
            ret = binanceLive.getBinanceExchange().getMarketDataService().getTicker(new CurrencyPair(base, counter)).getLast();
        } catch (IOException e) {
            LOG.error(e.getMessage());
        } catch (CurrencyPairNotValidException e) {
            LOG.info("Currency Pair {}/{} is not valid. Swapping.", base, counter);
        }

        if (ret == null) {
            try {
                ret = binanceLive.getBinanceExchange().getMarketDataService().getTicker(new CurrencyPair(counter, base)).getLast();
            } catch (IOException e) {
                LOG.error(e.getMessage());
            } catch (CurrencyPairNotValidException e) {
                LOG.info("Currency Pair {}/{} is not valid even after swapping. Return value is NULL, sorry.", counter, base);
            }
        }
        return ret;
    }

    public static BinanceLimitOrder createBinanceLimitOrder(Portfolio portfolio, LimitOrder limitOrder) {
        BinanceLimitOrder existingBinanceLimitOrder = binanceLimitOrderRepository.getById(limitOrder.getId());
        if (existingBinanceLimitOrder != null) {
            LOG.info("Order {} exists.", limitOrder.getId());
            return existingBinanceLimitOrder;
        }
        LOG.info("Creating new BinanceLimitOrder for order {} status {}", limitOrder.getId(), limitOrder.getStatus());
        BinanceLimitOrder binanceLimitOrder = new BinanceLimitOrder(limitOrder, portfolio);
        binanceLimitOrder.setStatus(org.knowm.xchange.dto.Order.OrderStatus.NEW);
        BinanceTransactions.processBinanceLimitOrder(binanceLimitOrder);
        binanceLimitOrderRepository.save(binanceLimitOrder);
        return binanceLimitOrder;
    }
}
