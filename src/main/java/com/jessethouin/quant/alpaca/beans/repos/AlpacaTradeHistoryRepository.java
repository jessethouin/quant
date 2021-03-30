package com.jessethouin.quant.alpaca.beans.repos;

import com.jessethouin.quant.alpaca.beans.AlpacaTradeHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlpacaTradeHistoryRepository extends CrudRepository<AlpacaTradeHistory, Long> {}
