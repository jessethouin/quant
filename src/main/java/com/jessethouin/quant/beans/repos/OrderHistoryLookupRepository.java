package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.OrderHistoryLookup;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderHistoryLookupRepository extends CrudRepository<OrderHistoryLookup, Long> {}
