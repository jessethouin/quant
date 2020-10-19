package com.jessethouin.quant.alpaca;

import net.jacobpeterson.domain.polygon.historictrades.HistoricTradesResponse;
import net.jacobpeterson.polygon.PolygonAPI;
import net.jacobpeterson.polygon.rest.exception.PolygonAPIRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PolygonIntradayTrades {
    private static final Logger LOG = LogManager.getLogger(PolygonIntradayTrades.class);

    public static void main(String[] args) {
        final PolygonAPI polygonAPI = new PolygonAPI();
        File csvOutputFile = new File("HistoricTrades.csv");

        try {
            long begin = LocalDateTime.of(2020, 10, 14, 3, 59, 0).atZone(ZoneId.of("America/New_York")).toInstant().toEpochMilli() * 1000;
            long end = LocalDateTime.of(2020, 10, 14, 20, 1, 0).atZone(ZoneId.of("America/New_York")).toInstant().toEpochMilli() * 1000;
            AtomicLong timestamp = new AtomicLong(begin);
            AtomicInteger c = new AtomicInteger();
            List<String> dataLines = new ArrayList<>();
            final String ticker = "AAPL";
            final int limit = 25000;

/*
            AggregatesResponse aggregates = polygonAPI.getAggregates("AAPL", 1, Timespan.MINUTE, LocalDate.of(2020, 10, 14), LocalDate.of(2020, 10, 14), true);
            aggregates.getResults().forEach(a -> {
                LOG.info(c.incrementAndGet() + " " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date(a.getT())) + " : " + a.getC());
            });
*/

            while (timestamp.get() < end) {

                HistoricTradesResponse aapl = polygonAPI.getHistoricTrades(ticker, LocalDate.of(2020, 10, 14), timestamp.get(), 1602720060000000000L, false, limit);
                aapl.getResults().forEach(t -> {
                    LOG.info(c.incrementAndGet() + " " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(new Date(t.getT() / 1000 / 1000)) + " : " + t.getP());
                    dataLines.add(String.join(",",
                            ticker,
                            t.getP().toString(),
                            Optional.ofNullable(t.getT()).orElse(0L).toString(),
                            Optional.ofNullable(t.getF()).orElse(0L).toString(),
                            t.getI(),
                            t.getS().toString(),
                            t.getQ().toString(),
                            t.getX().toString(),
                            t.getY().toString(),
                            t.getZ().toString()
                    ));
                    timestamp.set(t.getT());
                });

                try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
                    dataLines.forEach(pw::println);
                } catch (FileNotFoundException e) {
                    LOG.error(e.getLocalizedMessage());
                }

                if (aapl.getResultsCount() < limit) break;
            }
        } catch (PolygonAPIRequestException e) {
            LOG.error(e.getLocalizedMessage());
        }
    }
}
