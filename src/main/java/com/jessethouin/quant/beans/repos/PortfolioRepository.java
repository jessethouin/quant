package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    Portfolio getTop1ByPortfolioIdIsNotNullOrderByPortfolioIdDesc();
}
