package com.example.mcprice.controller;

import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.dto.MerchantSyncRunDto;
import com.example.mcprice.dto.PipelineRunDto;
import com.example.mcprice.dto.PublishRequest;
import com.example.mcprice.repository.MerchantSyncRunRepository;
import com.example.mcprice.service.MerchantSyncService;
import com.example.mcprice.domain.WebsitePublishRun;
import com.example.mcprice.dto.WebsitePublishRunDto;
import com.example.mcprice.repository.WebsitePublishRunRepository;
import com.example.mcprice.service.WebsitePublishService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/publish")
@RequiredArgsConstructor
public class PublishController {

    private final WebsitePublishService websitePublishService;
    private final MerchantSyncService merchantSyncService;
    private final WebsitePublishRunRepository websitePublishRunRepository;
    private final MerchantSyncRunRepository merchantSyncRunRepository;

    @PostMapping("/website-runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public WebsitePublishRunDto createWebsiteRun(@RequestBody(required = false) PublishRequest request) {
        return websitePublishService.publishApprovedRecommendations(request == null ? null : request.recommendationIds(), null);
    }

    @GetMapping("/website-runs/{id}")
    public WebsitePublishRunDto getWebsiteRun(@PathVariable Long id) {
        return websitePublishRunRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> NotFoundException.of("WebsitePublishRun", id));
    }

    @PostMapping("/merchant-runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public MerchantSyncRunDto createMerchantRun(@RequestBody(required = false) PublishRequest request) {
        return merchantSyncService.syncApprovedRecommendations(request == null ? null : request.recommendationIds(), null);
    }

    @GetMapping("/merchant-runs/{id}")
    public MerchantSyncRunDto getMerchantRun(@PathVariable Long id) {
        return merchantSyncRunRepository.findById(id)
                .map(r -> new MerchantSyncRunDto(r.getId(), r.getStatus().name(), r.isDryRun(), r.getTotalItems(),
                        r.getSuccessItems(), r.getFailedItems(), r.getStartedAt(), r.getFinishedAt()))
                .orElseThrow(() -> NotFoundException.of("MerchantSyncRun", id));
    }

    /** Chay ca 2 buoc website + merchant lien tiep, tra ve ket qua ca 2 (khong atomic — moi buoc doc lap). */
    @PostMapping("/pipeline-runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public PipelineRunDto createPipelineRun(@RequestBody(required = false) PublishRequest request) {
        List<Long> ids = request == null ? null : request.recommendationIds();
        WebsitePublishRunDto websiteRun = websitePublishService.publishApprovedRecommendations(ids, null);
        MerchantSyncRunDto merchantRun = merchantSyncService.syncApprovedRecommendations(ids, null);
        return new PipelineRunDto(websiteRun, merchantRun);
    }

    private WebsitePublishRunDto toDto(WebsitePublishRun r) {
        return new WebsitePublishRunDto(r.getId(), r.getStatus().name(), r.isDryRun(), r.getTotalItems(),
                r.getSuccessItems(), r.getFailedItems(), r.getStartedAt(), r.getFinishedAt());
    }
}
