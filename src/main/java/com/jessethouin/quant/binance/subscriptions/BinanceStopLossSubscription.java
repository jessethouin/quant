package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.conf.Config.CONFIG;

import com.jessethouin.quant.broker.Fundamental;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class BinanceStopLossSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceStopLossSubscription.class);
    private final Fundamental fundamental;

    public Disposable subscribe() {
        fundamental.setValue(Util.getValueAtPrice(fundamental.getBaseCurrency(), fundamental.getPrice()).add(fundamental.getCounterCurrency().getQuantity()));
        fundamental.setPreviousValue(fundamental.getValue());

        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getTicker(fundamental.getCurrencyPair()).subscribe(ticker -> {
            fundamental.setValue(Util.getValueAtPrice(fundamental.getBaseCurrency(), fundamental.getPrice()).add(fundamental.getCounterCurrency().getQuantity()));
            if (fundamental.getValue().compareTo(fundamental.getPreviousValue().multiply(CONFIG.getStopLoss())) < 0)
                Transactions.placeSellOrder(CONFIG.getBroker(), null, fundamental.getBaseCurrency(), fundamental.getCounterCurrency(), ticker.getLast());
            fundamental.setPreviousValue(fundamental.getValue());
        }, throwable -> LOG.error("Error in ticket subscription (stop loss)", throwable));

    }
}
