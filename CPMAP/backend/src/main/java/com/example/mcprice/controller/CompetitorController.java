package com.example.mcprice.controller;

import com.example.mcprice.dto.AddListingRequest;
import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.CompetitorDto;
import com.example.mcprice.dto.CompetitorUpsertRequest;
import com.example.mcprice.dto.CrawlResult;
import com.example.mcprice.dto.DiscoveryJobDto;
import com.example.mcprice.dto.TestCrawlRequest;
import com.example.mcprice.service.CompetitorService;
import com.example.mcprice.service.CompetitorCrawlTriggerService;
import com.example.mcprice.service.CompetitorDiscoveryTriggerService;
import com.example.mcprice.service.CrawlOrchestrationService;
import com.example.mcprice.service.MatchConfirmPriceService;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.service.MatchingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/competitors")
@RequiredArgsConstructor
public class CompetitorController {

    private final CompetitorService competitorService;
    private final CompetitorDiscoveryTriggerService discoveryTriggerService;
    private final CompetitorCrawlTriggerService crawlTriggerService;
    private final CompetitorListingRepository competitorListingRepository;
    private final MatchingService matchingService;
    private final MatchConfirmPriceService matchConfirmPriceService;
    private final CrawlOrchestrationService crawlOrchestrationService;

    @GetMapping
    public List<CompetitorDto> findAll() {
        return competitorService.findAll();
    }

    /**
     * Tao doi thu xong (transaction da commit) MOI kich hoat auto-discovery — goi luc con dang
     * @Transactional trong CompetitorService se gay race condition, luong @Async doc DB truoc
     * khi dong Competitor duoc commit va bao loi "Competitor khong ton tai".
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CompetitorDto create(@Valid @RequestBody CompetitorUpsertRequest request) {
        CompetitorDto created = competitorService.create(request);
        discoveryTriggerService.autoDiscoverOnCreate(created.id(), currentActor());
        return created;
    }

    /**
     * "Enabled" chi la co bat/tat co ban: doi thu bi tat se bi loai khoi cac job chay theo lich
     * (CompetitorDiscoveryJob, CrawlOrchestrationService scheduled) va khoi tinh gia trung binh
     * (PriceCalculationService da tu kiem tra competitor.isEnabled() khi chon nguon gia hop le).
     * KHONG tu kich hoat crawl/tinh lai gia khi bat/tat o day — muon doi chieu SKU + crawl gia
     * that ngay lap tuc, dung nut rieng POST /{id}/price-sync-jobs (xem createPriceSyncJob).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompetitorDto update(@PathVariable Long id, @Valid @RequestBody CompetitorUpsertRequest request) {
        return competitorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        competitorService.delete(id);
    }

    /** Tao 1 discovery job moi (thu cong) cho 1 doi thu, tra ve jobRunId de FE poll tien do. */
    @PostMapping("/{id}/discovery-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public DiscoveryJobDto createDiscoveryJob(@PathVariable Long id) {
        Long jobRunId = discoveryTriggerService.resolveSitemapAndTrigger(id, currentActor());
        return new DiscoveryJobDto(jobRunId);
    }

    /**
     * Doi chieu SKU danh sach URL da kham pha (sitemap) cua doi thu nay voi san pham hien co
     * (giong "Quet lai"), roi crawl gia THAT cho cac listing da khop chac chan (AUTO_CONFIRMED/
     * MANUALLY_CONFIRMED) va tinh lai gia trung binh cho cac san pham lien quan. Chay ngam,
     * tra ve jobRunId de FE poll tien do — day la nut thay the "Test crawl" cu.
     */
    @PostMapping("/{id}/price-sync-jobs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public DiscoveryJobDto createPriceSyncJob(@PathVariable Long id) {
        Long jobRunId = crawlTriggerService.triggerCrawlAndRecalculate(id, currentActor());
        return new DiscoveryJobDto(jobRunId);
    }

    /** Test crawl 1 URL cu the theo extractorConfig cua doi thu — khong luu ket qua vao DB. */
    @PostMapping("/{id}/crawl-tests")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public CrawlResult createCrawlTest(@PathVariable Long id, @Valid @RequestBody TestCrawlRequest request) {
        return crawlOrchestrationService.testCrawl(id, request.url());
    }

    /**
     * Danh sach URL/san pham da khop duoc voi doi thu nay qua sitemap discovery hoac crawl.
     * Loc theo SKU (tim gan dung, khong phan biet hoa/thuong) neu co truyen "sku".
     */
    @GetMapping("/{id}/listings")
    public PageResponse<CompetitorListingDto> listings(@PathVariable Long id,
                                                         @RequestParam(required = false) String sku,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        var pageable = PageRequest.of(page, size);
        var result = (StringUtils.hasText(sku)
                ? competitorListingRepository.findByCompetitorIdAndSkuContaining(id, sku.trim(), pageable)
                : competitorListingRepository.findByCompetitorId(id, pageable))
                .map(matchingService::toDto);
        return PageResponse.of(result);
    }

    /**
     * Them thu cong 1 URL vao danh sach cua doi thu — dung khi nguoi dung tu tim duoc URL dung
     * qua chuc nang "Tim theo SKU" tren giao dien ma pipeline tu dong khong khop duoc/khop sai.
     * Coi nhu da xac nhan khop ngay, tra ve 409 neu URL nay da co san trong danh sach.
     */
    @PostMapping("/{id}/listings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public CompetitorListingDto addListing(@PathVariable Long id, @Valid @RequestBody AddListingRequest request) {
        CompetitorListingDto created = matchingService.addManualListing(id, request.productId(), request.url());
        return matchConfirmPriceService.crawlPriceAndRecalculate(created);
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }
}
