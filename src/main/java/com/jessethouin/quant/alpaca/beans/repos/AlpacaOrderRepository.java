package com.jessethouin.quant.alpaca.beans.repos;

import com.jessethouin.quant.alpaca.beans.AlpacaOrder;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlpacaOrderRepository extends CrudRepository<AlpacaOrder, Long> {
    AlpacaOrder getById(String id);
}
