package com.example.mcprice.controller;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.dto.MerchantSyncRunDto;
import com.example.mcprice.repository.MerchantSyncRunRepository;
import com.example.mcprice.service.MerchantSyncService;
import com.example.mcprice.domain.WebsitePublishRun;
import com.example.mcprice.dto.WebsitePublishRunDto;
import com.example.mcprice.repository.WebsitePublishRunRepository;
import com.example.mcprice.service.WebsitePublishService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publish")
@RequiredArgsConstructor
public class PublishController {

    private final WebsitePublishService websitePublishService;
    private final MerchantSyncService merchantSyncService;
    private final WebsitePublishRunRepository websitePublishRunRepository;
    private final MerchantSyncRunRepository merchantSyncRunRepository;

    public record PublishRequest(List<Long> recommendationIds) {
    }

    @PostMapping("/website")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public WebsitePublishRunDto publishWebsite(@RequestBody(required = false) PublishRequest request) {
        return websitePublishService.publishApprovedRecommendations(request == null ? null : request.recommendationIds(), null);
    }

    @PostMapping("/merchant")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public MerchantSyncRunDto publishMerchant(@RequestBody(required = false) PublishRequest request) {
        return merchantSyncService.syncApprovedRecommendations(request == null ? null : request.recommendationIds(), null);
    }

    @PostMapping("/full-pipeline")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public Map<String, Object> fullPipeline(@RequestBody(required = false) PublishRequest request) {
        List<Long> ids = request == null ? null : request.recommendationIds();
        WebsitePublishRunDto websiteRun = websitePublishService.publishApprovedRecommendations(ids, null);
        MerchantSyncRunDto merchantRun = merchantSyncService.syncApprovedRecommendations(ids, null);
        return Map.of("website", websiteRun, "merchant", merchantRun);
    }

    @GetMapping("/runs/{id}")
    public Object getRun(@PathVariable Long id) {
        return websitePublishRunRepository.findById(id)
                .<Object>map(this::toDto)
                .or(() -> merchantSyncRunRepository.findById(id).map(r -> new MerchantSyncRunDto(r.getId(),
                        r.getStatus().name(), r.isDryRun(), r.getTotalItems(), r.getSuccessItems(), r.getFailedItems(),
                        r.getStartedAt(), r.getFinishedAt())))
                .orElseThrow(() -> NotFoundException.of("PublishRun", id));
    }

    private WebsitePublishRunDto toDto(WebsitePublishRun r) {
        return new WebsitePublishRunDto(r.getId(), r.getStatus().name(), r.isDryRun(), r.getTotalItems(),
                r.getSuccessItems(), r.getFailedItems(), r.getStartedAt(), r.getFinishedAt());
    }
}
