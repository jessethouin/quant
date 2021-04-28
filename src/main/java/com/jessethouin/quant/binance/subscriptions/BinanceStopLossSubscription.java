package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_MARKET_DATA_SERVICE;
import static com.jessethouin.quant.conf.Config.CONFIG;

import com.jessethouin.quant.broker.Fundamentals;
import com.jessethouin.quant.broker.Transactions;
import com.jessethouin.quant.broker.Util;
import io.reactivex.disposables.Disposable;
import lombok.Builder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Builder
public class BinanceStopLossSubscription {
    private static final Logger LOG = LogManager.getLogger(BinanceStopLossSubscription.class);
    private final Fundamentals fundamentals;

    public Disposable subscribe() {
        fundamentals.setValue(Util.getValueAtPrice(fundamentals.getBaseCurrency(), fundamentals.getPrice()).add(fundamentals.getCounterCurrency().getQuantity()));
        fundamentals.setPreviousValue(fundamentals.getValue());

        return BINANCE_STREAMING_MARKET_DATA_SERVICE.getTicker(fundamentals.getCurrencyPair()).subscribe(ticker -> {
            fundamentals.setValue(Util.getValueAtPrice(fundamentals.getBaseCurrency(), fundamentals.getPrice()).add(fundamentals.getCounterCurrency().getQuantity()));
            if (fundamentals.getValue().compareTo(fundamentals.getPreviousValue().multiply(CONFIG.getStopLoss())) < 0)
                Transactions.placeSellOrder(CONFIG.getBroker(), null, fundamentals.getBaseCurrency(), fundamentals.getCounterCurrency(), ticker.getLast());
            fundamentals.setPreviousValue(fundamentals.getValue());
        }, throwable -> LOG.error("Error in ticket subscription (stop loss)", throwable));

    }
}
