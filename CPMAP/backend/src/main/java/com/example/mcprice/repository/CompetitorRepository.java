package com.example.mcprice.repository;

import com.example.mcprice.domain.Competitor;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompetitorRepository extends JpaRepository<Competitor, Long> {
    Optional<Competitor> findByNameIgnoreCase(String name);

    List<Competitor> findByEnabledTrue();
}
