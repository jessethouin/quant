package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.Portfolio;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends CrudRepository<Portfolio, Long> {
    Portfolio getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc();
}
