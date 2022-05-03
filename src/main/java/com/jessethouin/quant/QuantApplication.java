package com.jessethouin.quant;

import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.backtest.BacktestStaticParameters;
import com.jessethouin.quant.binance.BinanceCaptureHistory;
import com.jessethouin.quant.binance.BinanceLive;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

import static com.jessethouin.quant.conf.Config.CONFIG;

@Getter
@SpringBootApplication
@EnableScheduling
@PropertySource(name = "quantProperties", value = "/quant.properties")
public class QuantApplication {
    private static final Logger LOG = LogManager.getLogger(QuantApplication.class);
    private static AlpacaLive alpacaLive;
    private static BinanceLive binanceLive;
    private static BacktestParameterCombos backtestParameterCombos;
    private static BacktestStaticParameters backtestStaticParameters;
    private static BinanceCaptureHistory binanceCaptureHistory;

    public QuantApplication(AlpacaLive alpacaLive, BinanceLive binanceLive, BacktestParameterCombos backtestParameterCombos, BacktestStaticParameters backtestStaticParameters, BinanceCaptureHistory binanceCaptureHistory) {
        QuantApplication.alpacaLive = alpacaLive;
        QuantApplication.binanceLive = binanceLive;
        QuantApplication.backtestParameterCombos = backtestParameterCombos;
        QuantApplication.backtestStaticParameters = backtestStaticParameters;
        QuantApplication.binanceCaptureHistory = binanceCaptureHistory;
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
                case "combos" -> {
                    CONFIG.setBackTest(true);
                    backtestParameterCombos.findBestCombos(Arrays.copyOfRange(args, 1, args.length));
                }
                case "backtest" -> {
                    CONFIG.setBackTest(true);
                    backtestStaticParameters.runBacktest();
                }
                case "paper" -> alpacaLive.doLive();
                case "binance" -> binanceLive.doLive();
                case "capture" -> binanceCaptureHistory.doCapture();
                default -> LOG.error("1st arg must be \"combos\", \"backtest\", \"paper\", or \"binance\".");
            }
        }
    }
}
