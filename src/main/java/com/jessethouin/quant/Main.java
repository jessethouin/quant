package com.jessethouin.quant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        Backtest.findBestCombos(args); // s 60 : l 84 : rl 0.10 : rh 0.01
    }
}
