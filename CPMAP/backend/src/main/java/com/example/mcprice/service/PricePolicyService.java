package com.example.mcprice.service;

import com.example.mcprice.domain.OutlierStrategy;
import com.example.mcprice.domain.PolicyScope;
import com.example.mcprice.domain.PricePolicy;
import com.example.mcprice.dto.PricePolicyDto;
import com.example.mcprice.dto.PricePolicyUpdateRequest;
import com.example.mcprice.repository.PricePolicyRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PricePolicyService {

    private final PricePolicyRepository pricePolicyRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PricePolicyDto getGlobalPolicy() {
        return toDto(pricePolicyRepository.findByScope(PolicyScope.GLOBAL)
                .orElseGet(() -> pricePolicyRepository.save(PricePolicy.builder().scope(PolicyScope.GLOBAL).build())));
    }

    public PricePolicyDto updateGlobalPolicy(PricePolicyUpdateRequest request) {
        PricePolicy policy = pricePolicyRepository.findByScope(PolicyScope.GLOBAL)
                .orElseGet(() -> PricePolicy.builder().scope(PolicyScope.GLOBAL).build());
        apply(policy, request);
        PricePolicy saved = pricePolicyRepository.save(policy);
        auditService.record("PRICE_POLICY_UPDATE", "PRICE_POLICY", "GLOBAL",
                Map.of("minimumCompetitorCount", saved.getMinimumCompetitorCount(), "roundingStep", saved.getRoundingStep().toPlainString()));
        return toDto(saved);
    }

    private void apply(PricePolicy policy, PricePolicyUpdateRequest request) {
        policy.setMinimumCompetitorCount(request.minimumCompetitorCount());
        policy.setMaxObservationAgeHours(request.maxObservationAgeHours());
        policy.setRoundingStep(request.roundingStep());
        policy.setMaxIncreasePercent(request.maxIncreasePercent());
        policy.setMaxDecreasePercent(request.maxDecreasePercent());
        policy.setOutlierThresholdPercent(request.outlierThresholdPercent());
        policy.setOutlierStrategy(OutlierStrategy.valueOf(request.outlierStrategy()));
        policy.setRequireManualApproval(request.requireManualApproval());
        policy.setMinimumAllowedPrice(request.minimumAllowedPrice());
        policy.setMaximumAllowedPrice(request.maximumAllowedPrice());
        policy.setAutoPublishEnabled(request.autoPublishEnabled());
    }

    private PricePolicyDto toDto(PricePolicy p) {
        return new PricePolicyDto(p.getId(), p.getScope().name(), p.getCategory(),
                p.getProduct() == null ? null : p.getProduct().getId(), p.getMinimumCompetitorCount(),
                p.getMaxObservationAgeHours(), p.getRoundingStep(), p.getMaxIncreasePercent(), p.getMaxDecreasePercent(),
                p.getOutlierThresholdPercent(), p.getOutlierStrategy().name(), p.isRequireManualApproval(),
                p.getMinimumAllowedPrice(), p.getMaximumAllowedPrice(), p.isAutoPublishEnabled());
    }
}
