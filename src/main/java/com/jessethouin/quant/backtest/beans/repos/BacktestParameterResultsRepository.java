package com.jessethouin.quant.backtest.beans.repos;

import com.jessethouin.quant.backtest.beans.BacktestParameterResults;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestParameterResultsRepository extends CrudRepository<BacktestParameterResults, Long> {}
