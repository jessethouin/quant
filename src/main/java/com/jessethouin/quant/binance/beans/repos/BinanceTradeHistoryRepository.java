package com.jessethouin.quant.binance.beans.repos;

import com.jessethouin.quant.binance.beans.BinanceTradeHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BinanceTradeHistoryRepository extends CrudRepository<BinanceTradeHistory, Long> {
    List<BinanceTradeHistory> getBinanceTradeHistoriesByTimestampBetween(Date start, Date end);

    @Query(value = "select max(timestamp) from BinanceTradeHistory")
    Date getMaxTimestamp();
}
