package com.example.mcprice.service;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.repository.CompetitorListingRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Kham pha URL san pham cua doi thu qua sitemap.xml (hop le theo robots.txt) roi ghep voi
 * san pham cua minh bang chinh ProductMatchingService (buoc "SKU/model trong URL slug") —
 * tai su dung dung pipeline ghep san pham da co, khong tao logic ghep rieng.
 *
 * "Tong so URL lay duoc" (totalUrlsScanned / Competitor.lastSitemapUrlCount) la TOAN BO URL
 * san pham cua doi thu sau khi SitemapFetcher da loc bo cac sitemap khong phai san pham theo
 * ten file — CHU DICH khong doi chieu SKU/ten san pham de co con so nhanh (chi doc sitemap,
 * khong can tai tung trang san pham rieng le). Doi chieu SKU van chay o duoi (vong for) de
 * cap nhat competitor_listings phuc vu crawl gia, nhung khong lam cham/anh huong den con so
 * "tong URL lay duoc" nay.
 */
@Service
@RequiredArgsConstructor
public class SitemapDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SitemapDiscoveryService.class);

    private static final int PROGRESS_UPDATE_INTERVAL = 25;

    private final SitemapFetcher sitemapFetcher;
    private final CompetitorRepository competitorRepository;
    private final SitemapMatchExecutor sitemapMatchExecutor;
    private final AppProperties appProperties;
    private final AuditService auditService;
    private final JobRunService jobRunService;
    private final CompetitorListingRepository competitorListingRepository;

    public record DiscoveryResult(int totalUrlsScanned, int autoConfirmed, int reviewRequired, int conflicts, int noMatch) {
    }

    public DiscoveryResult discoverForCompetitor(Long competitorId) {
        return discoverForCompetitor(competitorId, null);
    }

    /**
     * jobRunId khac null khi duoc goi tu mot tien trinh ngam theo doi rieng cho 1 doi thu (xem
     * CompetitorDiscoveryAsyncExecutor) — cho phep bao cao % tien do theo tung URL da xu ly,
     * thay vi chi 0% luc chay va 100% luc xong nhu job quet tat ca doi thu cung luc.
     */
    public DiscoveryResult discoverForCompetitor(Long competitorId, Long jobRunId) {
        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> new IllegalArgumentException("Competitor #" + competitorId + " khong ton tai"));
        Object sitemapUrlConfig = competitor.getExtractorConfig().get("sitemapUrl");
        if (sitemapUrlConfig == null) {
            log.warn("Competitor {} chua cau hinh sitemapUrl, bo qua discovery", competitor.getName());
            return new DiscoveryResult(0, 0, 0, 0, 0);
        }

        String userAgent = appProperties.getSecurity().getCrawlUserAgent();
        List<String> urls = sitemapFetcher.fetchAllUrls(sitemapUrlConfig.toString(), competitorId,
                competitor.getRequestsPerMinute(), userAgent);
        log.info("Sitemap discovery cho {}: tim thay {} URL, bat dau ghep SKU", competitor.getName(), urls.size());

        // Doc sitemap thanh cong nghia la da crawl duoc website doi thu — cap nhat lastSuccessAt
        // du buoc nay chua lay gia (chi tim URL), vi nguoi dung coi day la "lan crawl web doi
        // thu gan nhat", khong chi tinh rieng buoc crawl gia tung trang san pham.
        competitor.setLastSitemapUrlCount(urls.size());
        competitor.setLastSuccessAt(OffsetDateTime.now());
        competitorRepository.save(competitor);

        int staleRemoved = clearStaleListings(competitorId, urls);

        int autoConfirmed = 0;
        int reviewRequired = 0;
        int conflicts = 0;
        int noMatch = 0;

        for (int i = 0; i < urls.size(); i++) {
            SitemapMatchExecutor.Outcome outcome;
            try {
                outcome = sitemapMatchExecutor.matchAndUpsertOne(competitorId, urls.get(i));
            } catch (Exception e) {
                log.warn("Loi ghep URL {} cho competitor #{}: {}", urls.get(i), competitorId, e.getMessage());
                outcome = SitemapMatchExecutor.Outcome.NO_MATCH;
            }
            switch (outcome) {
                case AUTO_CONFIRMED -> autoConfirmed++;
                case REVIEW_REQUIRED -> reviewRequired++;
                case CONFLICT -> conflicts++;
                case NO_MATCH -> noMatch++;
            }
            if (jobRunId != null && ((i + 1) % PROGRESS_UPDATE_INTERVAL == 0 || i == urls.size() - 1)) {
                jobRunService.updateProgress(jobRunId, urls.size(), i + 1, 0);
            }
        }

        auditService.record("COMPETITOR_SITEMAP_DISCOVERY", "COMPETITOR", String.valueOf(competitorId),
                Map.of("totalUrlsScanned", urls.size(), "autoConfirmed", autoConfirmed,
                        "reviewRequired", reviewRequired, "conflicts", conflicts, "staleRemoved", staleRemoved));
        return new DiscoveryResult(urls.size(), autoConfirmed, reviewRequired, conflicts, noMatch);
    }

    /**
     * Xoa cac competitor_listings cu KHONG con xuat hien trong lan quet sitemap moi nhat — vd
     * san pham da bi go khoi website doi thu, hoac la du lieu sai tu mot lan quet loi truoc do
     * (nhu vu sitemap blog lan vao sitemap san pham). Nhung URL VAN con trong sitemap se duoc
     * upsert lai binh thuong ben duoi (giu nguyen id, khong mat lich su gia da crawl).
     */
    private int clearStaleListings(Long competitorId, List<String> freshUrls) {
        Set<String> freshUrlSet = new HashSet<>(freshUrls);
        List<CompetitorListing> existing = competitorListingRepository.findAllByCompetitorId(competitorId);
        List<Long> staleIds = existing.stream()
                .filter(l -> !freshUrlSet.contains(l.getUrl()))
                .map(CompetitorListing::getId)
                .toList();
        if (!staleIds.isEmpty()) {
            competitorListingRepository.deleteAllByIdInBatch(staleIds);
            log.info("Da xoa {} URL cu khong con trong sitemap moi cua competitor #{}", staleIds.size(), competitorId);
        }
        return staleIds.size();
    }
}
