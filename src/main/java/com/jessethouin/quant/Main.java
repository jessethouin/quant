package com.jessethouin.quant;

import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.alpaca.Live;
import com.jessethouin.quant.backtest.BacktestStaticParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0) {
            switch (args[0]) {
                case "combos" -> BacktestParameterCombos.findBestCombos(Arrays.copyOfRange(args, 1, args.length));
                case "backtest" -> BacktestStaticParameters.runBacktest(Arrays.copyOfRange(args, 1, args.length));
                case "live" -> Live.doPaperTrading();
                default -> LOG.error("1st arg must be \"combos\", \"backtest\", or \"live\".");
            }
        }
    }
}
