package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.CurrencyLedger;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyLedgerRepository extends CrudRepository<CurrencyLedger, Long> {}
