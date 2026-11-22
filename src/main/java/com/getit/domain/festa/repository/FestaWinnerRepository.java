package com.getit.domain.festa.repository;

import com.getit.domain.festa.entity.FestaWinner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestaWinnerRepository extends JpaRepository<FestaWinner, Long> {

  List<FestaWinner> findAllByOrderByDrawOrderAsc();
}
