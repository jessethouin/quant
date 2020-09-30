package com.jessethouin.quant;

import com.jessethouin.quant.backtest.Backtest;
import com.jessethouin.quant.alpaca.Live;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        if (args.length > 0) {
            switch (args[0]) {
                case "backtest" -> Backtest.findBestCombos(Arrays.copyOfRange(args, 1, args.length));
                case "live" -> Live.doPaperTrading(Arrays.copyOfRange(args, 1, args.length));
                default -> LOG.error("1st arg must be \"backtest\" or \"live\".");
            }
        }

    }
}
