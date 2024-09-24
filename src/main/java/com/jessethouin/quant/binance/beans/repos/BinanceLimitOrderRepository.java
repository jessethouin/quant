package com.jessethouin.quant.binance.beans.repos;

import com.jessethouin.quant.binance.beans.BinanceLimitOrder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BinanceLimitOrderRepository extends CrudRepository<BinanceLimitOrder, Long> {
    BinanceLimitOrder getById(String id);
}