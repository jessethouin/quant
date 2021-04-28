package com.jessethouin.quant.binance.subscriptions;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_TRADE_SERVICE;

import com.jessethouin.quant.binance.BinanceStreamProcessing;
import io.reactivex.disposables.Disposable;
import lombok.Builder;

@Builder
public class BinanceExecutionReportsSubscription {
    public Disposable subscribe() {
        return BINANCE_STREAMING_TRADE_SERVICE.getRawExecutionReports().subscribe(BinanceStreamProcessing::processRemoteOrder);
    }
}
