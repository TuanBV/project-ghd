package com.example.mcprice.service;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.util.SkuNormalizer;
import com.example.mcprice.domain.AliasType;
import com.example.mcprice.domain.Product;
import com.example.mcprice.domain.ProductAlias;
import com.example.mcprice.dto.AliasCreateRequest;
import com.example.mcprice.dto.AliasDto;
import com.example.mcprice.repository.ProductAliasRepository;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductAliasService {

    private final ProductAliasRepository productAliasRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    /** Sinh alias tu SKU raw (ngoac don, hau to phien ban) va luu, bo qua neu da ton tai. */
    public List<ProductAlias> generateFromSku(Product product) {
        if (product.getSkuOriginal() == null || product.getSkuOriginal().isBlank()) {
            return List.of();
        }
        List<SkuNormalizer.AliasCandidate> candidates = SkuNormalizer.generateAliasCandidates(product.getSkuOriginal());
        return candidates.stream()
                .filter(c -> !productAliasRepository.existsByProductIdAndAliasTypeAndAliasNormalized(
                        product.getId(), AliasType.MODEL, c.normalizedValue()))
                .map(c -> productAliasRepository.save(ProductAlias.builder()
                        .product(product)
                        .aliasType(AliasType.MODEL)
                        .aliasOriginal(c.rawValue())
                        .aliasNormalized(c.normalizedValue())
                        .confirmed(c.confirmed())
                        .confidence(BigDecimal.valueOf(c.confidence()))
                        .build()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AliasDto> findByProduct(Long productId) {
        return productAliasRepository.findByProductId(productId).stream().map(this::toDto).toList();
    }

    public AliasDto create(Long productId, AliasCreateRequest request) {
        Product product = productRepository.findById(productId).orElseThrow(() -> NotFoundException.of("Product", productId));
        String normalized = SkuNormalizer.normalize(request.aliasOriginal());
        ProductAlias alias = productAliasRepository.save(ProductAlias.builder()
                .product(product)
                .aliasType(AliasType.valueOf(request.aliasType()))
                .aliasOriginal(request.aliasOriginal())
                .aliasNormalized(normalized)
                .confirmed(request.confirmed())
                .confidence(BigDecimal.valueOf(request.confirmed() ? 1.0 : 0.5))
                .build());
        auditService.record("ALIAS_CREATE", "PRODUCT", String.valueOf(productId),
                Map.of("aliasNormalized", normalized, "confirmed", alias.isConfirmed()));
        return toDto(alias);
    }

    private AliasDto toDto(ProductAlias alias) {
        return new AliasDto(alias.getId(), alias.getAliasType().name(), alias.getAliasOriginal(),
                alias.getAliasNormalized(), alias.isConfirmed(), alias.getConfidence());
    }
}
