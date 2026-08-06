package com.example.mcprice.service;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CrawlMode;
import com.example.mcprice.dto.CompetitorDto;
import com.example.mcprice.dto.CompetitorUpsertRequest;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.exception.ConflictException;
import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.repository.JobRunRepository;
import com.example.mcprice.repository.CompetitorListingRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CompetitorService {

    /** Website cua chinh minh, khong duoc phep tao lam doi thu. */
    public static final String OWN_WEBSITE_DOMAIN = "tongkhodienmaymienbac.com";

    private final CompetitorRepository competitorRepository;
    private final AuditService auditService;
    private final JobRunRepository jobRunRepository;
    private final CompetitorListingRepository competitorListingRepository;

    @Transactional(readOnly = true)
    public List<CompetitorDto> findAll() {
        return competitorRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * CHI luu ban ghi Competitor — KHONG kich hoat auto-discovery o day. Discovery phai duoc
     * kich hoat SAU KHI method nay tra ve va transaction da commit (xem
     * CompetitorController.create() goi CompetitorDiscoveryTriggerService.autoDiscoverOnCreate
     * sau do), neu khong luong @Async se doc DB truoc khi dong nay duoc commit va bao loi
     * "Competitor khong ton tai".
     */
    public CompetitorDto create(CompetitorUpsertRequest request) {
        String baseUrl = normalizeBaseUrl(request.baseUrl());
        guardNotOwnWebsite(baseUrl, request.name());
        competitorRepository.findByNameIgnoreCase(request.name()).ifPresent(c -> {
            throw new ConflictException("Doi thu '" + request.name() + "' da ton tai");
        });
        Competitor competitor = Competitor.builder()
                .name(request.name())
                .baseUrl(baseUrl)
                .enabled(request.enabled())
                .crawlMode(CrawlMode.valueOf(request.crawlMode()))
                .requestsPerMinute(request.requestsPerMinute())
                .timeoutSeconds(request.timeoutSeconds())
                .extractorConfig(request.extractorConfig() == null ? Map.of() : request.extractorConfig())
                .build();
        Competitor saved = competitorRepository.save(competitor);
        auditService.record("COMPETITOR_CREATE", "COMPETITOR", String.valueOf(saved.getId()),
                Map.of("name", saved.getName(), "baseUrl", saved.getBaseUrl()));
        return toDto(saved);
    }

    public CompetitorDto update(Long id, CompetitorUpsertRequest request) {
        String baseUrl = normalizeBaseUrl(request.baseUrl());
        guardNotOwnWebsite(baseUrl, request.name());
        Competitor competitor = getOrThrow(id);
        competitor.setName(request.name());
        competitor.setBaseUrl(baseUrl);
        competitor.setEnabled(request.enabled());
        competitor.setCrawlMode(CrawlMode.valueOf(request.crawlMode()));
        competitor.setRequestsPerMinute(request.requestsPerMinute());
        competitor.setTimeoutSeconds(request.timeoutSeconds());
        // Neu client khong gui extractorConfig (vd form cu chua co truong nay), GIU LAI cau hinh
        // hien tai thay vi xoa trang — tranh mat sitemapUrl/selector da cau hinh khi chi sua ten/enabled.
        if (request.extractorConfig() != null) {
            competitor.setExtractorConfig(request.extractorConfig());
        }
        Competitor saved = competitorRepository.save(competitor);
        auditService.record("COMPETITOR_UPDATE", "COMPETITOR", String.valueOf(saved.getId()),
                Map.of("name", saved.getName(), "enabled", saved.isEnabled()));
        return toDto(saved);
    }

    public void delete(Long id) {
        Competitor competitor = getOrThrow(id);
        competitorRepository.delete(competitor);
        auditService.record("COMPETITOR_DELETE", "COMPETITOR", String.valueOf(id), Map.of("name", competitor.getName()));
    }

    @Transactional(readOnly = true)
    public Competitor getOrThrow(Long id) {
        return competitorRepository.findById(id).orElseThrow(() -> NotFoundException.of("Competitor", id));
    }

    /**
     * Nguoi dung thuong go chi domain (vd "dienmaythienphu.vn") ma khong ghi "https://" — neu
     * giu nguyen, URI.create(baseUrl).getHost() se tra ve null (URI khong co scheme duoc Java
     * hieu la duong dan tuong doi, khong co host), lam hong het buoc kiem tra allowed-crawl-
     * domains va tao URL sitemap (vd "dienmaythienphu.vn/sitemap.xml" khong the ket noi duoc).
     * Tu dong them "https://" o day de tranh loi am tham nay ngay tu luc luu.
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return null;
        }
        String trimmed = baseUrl.trim();
        if (trimmed.isEmpty() || trimmed.matches("(?i)^https?://.*")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    private void guardNotOwnWebsite(String baseUrl, String name) {
        String normalized = baseUrl == null ? "" : baseUrl.toLowerCase();
        if (normalized.contains(OWN_WEBSITE_DOMAIN) || OWN_WEBSITE_DOMAIN.equalsIgnoreCase(name)) {
            throw new ConflictException(OWN_WEBSITE_DOMAIN + " la website cua minh, khong duoc them nhu doi thu");
        }
    }

    private CompetitorDto toDto(Competitor c) {
        var lastRun = jobRunRepository.findFirstByJobKeyOrderByCreatedAtDesc(
                CompetitorDiscoveryTriggerService.jobKeyFor(c.getId()));
        long matchedCount = competitorListingRepository.countByCompetitorId(c.getId());
        // discoveredUrlCount = TONG so URL san pham lay duoc tu sitemap (da loc file khong phai
        // san pham theo ten file), KHONG can doi chieu SKU — nhanh, chi doc sitemap.
        // matchedProductCount = trong so do, bao nhieu da khop duoc voi SKU/san pham cua ban.
        return new CompetitorDto(c.getId(), c.getName(), c.getBaseUrl(), c.isEnabled(), c.getCrawlMode().name(),
                c.getRequestsPerMinute(), c.getTimeoutSeconds(), c.getExtractorConfig(),
                c.getLastSuccessAt(), c.getLastErrorAt(), c.getLastErrorMessage(),
                lastRun.map(r -> r.getId()).orElse(null),
                lastRun.map(r -> r.getStatus().name()).orElse(null),
                lastRun.map(r -> r.getProgressPercent()).orElse(null),
                c.getLastSitemapUrlCount(), matchedCount);
    }
}
