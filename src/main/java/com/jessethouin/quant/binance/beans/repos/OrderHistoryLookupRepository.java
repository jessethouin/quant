package com.jessethouin.quant.binance.beans.repos;

import com.jessethouin.quant.binance.beans.OrderHistoryLookup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderHistoryLookupRepository extends CrudRepository<OrderHistoryLookup, Long> {}
