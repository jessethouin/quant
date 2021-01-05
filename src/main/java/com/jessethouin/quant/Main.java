package com.jessethouin.quant;

import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.backtest.BacktestStaticParameters;
import com.jessethouin.quant.binance.BinanceLive;
import com.jessethouin.quant.conf.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        if (Config.INSTANCE.getBackTest()) LOG.info("""
                
                ==============================================================================================
                =                                                                                            =
                =                                                                                            =
                =                       BACK TESTING IN PROGRESS - NO DATABASE WRITES                        =
                =                                                                                            =
                =                                                                                            =
                ==============================================================================================
                """);

        if (args.length > 0) {
            switch (args[0]) {
                case "combos" -> BacktestParameterCombos.findBestCombos(Arrays.copyOfRange(args, 1, args.length));
                case "backtest" -> BacktestStaticParameters.runBacktest();
                case "paper" -> AlpacaLive.doPaperTrading();
                case "binance" -> BinanceLive.doLive();
                default -> LOG.error("1st arg must be \"combos\", \"backtest\", \"paper\", or \"binance\".");
            }
        }
    }
}
