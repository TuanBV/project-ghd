package com.example.mcprice.repository;

import com.example.mcprice.domain.AliasType;
import com.example.mcprice.domain.ProductAlias;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductAliasRepository extends JpaRepository<ProductAlias, Long> {

    List<ProductAlias> findByProductId(Long productId);

    Optional<ProductAlias> findFirstByAliasNormalizedAndConfirmedTrue(String aliasNormalized);

    List<ProductAlias> findByAliasNormalized(String aliasNormalized);

    boolean existsByProductIdAndAliasTypeAndAliasNormalized(Long productId, AliasType aliasType, String aliasNormalized);
}
