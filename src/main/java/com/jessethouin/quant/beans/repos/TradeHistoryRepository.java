package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.TradeHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TradeHistoryRepository extends CrudRepository<TradeHistory, Long> {
    List<TradeHistory> getTradeHistoriesByTimestampBetween(Date start, Date end);
    Long countTradeHistoriesByTimestampBetween(Date start, Date end);

    @Query(value = "select max(timestamp) from TradeHistory")
    Date getMaxTimestamp();
}
