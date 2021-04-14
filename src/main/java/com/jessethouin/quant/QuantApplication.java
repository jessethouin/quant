package com.jessethouin.quant;

import static com.jessethouin.quant.conf.Config.CONFIG;

import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.backtest.BacktestStaticParameters;
import com.jessethouin.quant.binance.BinanceLive;
import java.util.Arrays;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Getter
@SpringBootApplication
@EnableScheduling
@PropertySource(name = "quantProperties", value = "/quant.properties")
public class QuantApplication {
    private static final Logger LOG = LogManager.getLogger(QuantApplication.class);
    private static BinanceLive binanceLive;

    public QuantApplication(BinanceLive binanceLive) {
        QuantApplication.binanceLive = binanceLive;
    }

    public static void main(String[] args) {
        SpringApplication.run(QuantApplication.class, args);

        if (CONFIG.isBackTest()) LOG.info("""
                
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
                case "binance" -> binanceLive.doLive();
                default -> LOG.error("1st arg must be \"combos\", \"backtest\", \"paper\", or \"binance\".");
            }
        }
    }
}
