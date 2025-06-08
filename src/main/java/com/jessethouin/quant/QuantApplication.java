package com.jessethouin.quant;

import com.jessethouin.quant.alpaca.AlpacaLive;
import com.jessethouin.quant.backtest.BacktestParameterCombos;
import com.jessethouin.quant.backtest.BacktestStaticParameters;
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
    private static BacktestParameterCombos backtestParameterCombos;
    private static BacktestStaticParameters backtestStaticParameters;

    public QuantApplication(AlpacaLive alpacaLive, BacktestParameterCombos backtestParameterCombos, BacktestStaticParameters backtestStaticParameters) {
        QuantApplication.alpacaLive = alpacaLive;
        QuantApplication.backtestParameterCombos = backtestParameterCombos;
        QuantApplication.backtestStaticParameters = backtestStaticParameters;
    }

    public static void main(String[] args) {
        SpringApplication.run(QuantApplication.class, args);

        if (args.length > 0) {
            switch (args[0]) {
                case "combos" -> {
                    CONFIG.setBackTest(true);
                    logBacktestWarning();
                    backtestParameterCombos.findBestCombos(Arrays.copyOfRange(args, 1, args.length));
                }
                case "backtest" -> {
                    CONFIG.setBackTest(true);
                    logBacktestWarning();
                    backtestStaticParameters.runBacktest();
                }
                case "alpaca" -> alpacaLive.doLive();
                default -> LOG.error("1st arg must be \"combos\", \"backtest\", or \"alpaca\".");
            }
        }
    }

    private static void logBacktestWarning() {
        LOG.info("""
                
                ==============================================================================================
                =                                                                                            =
                =                                                                                            =
                =                       BACK TESTING IN PROGRESS - NO DATABASE WRITES                        =
                =                                                                                            =
                =                                                                                            =
                ==============================================================================================
                """);
    }
}
