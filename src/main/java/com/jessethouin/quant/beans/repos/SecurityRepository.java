package com.jessethouin.quant.beans.repos;

import com.jessethouin.quant.beans.Security;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityRepository extends CrudRepository<Security, Long> {}
