package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.SecurityPosition;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityPositionRepository extends CrudRepository<SecurityPosition, Long> {}
