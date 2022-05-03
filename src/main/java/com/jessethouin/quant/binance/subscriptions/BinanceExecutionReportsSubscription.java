package com.jessethouin.quant.binance.subscriptions;

import com.jessethouin.quant.binance.BinanceStreamProcessor;
import io.reactivex.disposables.Disposable;
import lombok.Builder;

import static com.jessethouin.quant.binance.config.BinanceExchangeServices.BINANCE_STREAMING_TRADE_SERVICE;

@Builder
public class BinanceExecutionReportsSubscription {
    public Disposable subscribe() {
        return BINANCE_STREAMING_TRADE_SERVICE.getRawExecutionReports().subscribe(BinanceStreamProcessor::processRemoteOrder);
    }
}
