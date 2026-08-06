package com.example.mcprice.repository;

import com.example.mcprice.domain.PolicyScope;
import com.example.mcprice.domain.PricePolicy;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PricePolicyRepository extends JpaRepository<PricePolicy, Long> {

    Optional<PricePolicy> findByScope(PolicyScope scope);

    Optional<PricePolicy> findByScopeAndCategory(PolicyScope scope, String category);

    Optional<PricePolicy> findByScopeAndProductId(PolicyScope scope, Long productId);
}
