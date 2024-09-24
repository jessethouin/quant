package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.Currency;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends CrudRepository<Currency, Long> {}
