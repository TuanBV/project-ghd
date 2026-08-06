package com.example.mcprice.service;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.config.AppProperties;
import com.example.mcprice.domain.JobRun;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Diem vao de kich hoat sitemap discovery cho MOT doi thu (khi tao moi hoac bam "kham pha lai"
 * thu cong), khac voi CompetitorDiscoveryJob (job Quartz quet TAT CA doi thu theo lich).
 * Moi doi thu co job_key rieng (JOB_KEY_PREFIX + id) de khong bi khoa "dang chay" lan nhau va
 * khong xung dot voi job quet toan bo chay theo lich.
 *
 * QUAN TRONG: bean nay CO CHU Y khong co @Transactional bao ngoai. Neu goi cac method o day
 * tu trong mot transaction dang mo (vd goi truc tiep tu trong CompetitorService.create() luc
 * con @Transactional), luong @Async se doc DB o MOT KET NOI KHAC va co the KHONG THAY duoc
 * dong Competitor vua tao (transaction ngoai chua commit) — gay loi "Competitor khong ton tai"
 * ngay lap tuc. Vi vay cac method o day CHI duoc goi SAU KHI request tao/sua doi thu da hoan
 * tat (o Controller, khong o Service dang @Transactional).
 */
@Service
@RequiredArgsConstructor
public class CompetitorDiscoveryTriggerService {

    private static final Logger log = LoggerFactory.getLogger(CompetitorDiscoveryTriggerService.class);

    public static final String JOB_KEY_PREFIX = "CompetitorDiscoveryJob:";
    private static final String SITEMAP_URL_KEY = "sitemapUrl";

    private final JobRunService jobRunService;
    private final CompetitorDiscoveryAsyncExecutor asyncExecutor;
    private final CompetitorRepository competitorRepository;
    private final AppProperties appProperties;
    private final SitemapUrlResolver sitemapUrlResolver;

    /** Kich hoat truc tiep, GIA DINH sitemapUrl da co san trong extractorConfig. */
    public Long trigger(Long competitorId, String triggeredBy) {
        JobRun run = jobRunService.createQueued(JOB_KEY_PREFIX + competitorId, "MANUAL", triggeredBy);
        asyncExecutor.runDiscovery(competitorId, run.getId());
        return run.getId();
    }

    /**
     * Dung khi nguoi dung bam "Quet lai" thu cong — neu extractorConfig chua co sitemapUrl (vd
     * lan tao truoc do domain chua hop le, hoac chua tim duoc sitemap), se thu tim lai truoc
     * khi kich hoat. Nho vay nut "Quet lai" co the tu phuc hoi sau khi loi ban dau (vd baseUrl
     * thieu "https://") da duoc sua, khong can nguoi dung tu tay dien sitemapUrl.
     *
     * Neu khong the resolve duoc sitemap, tao mot JobRun FAILED voi ly do ro rang — KHONG de
     * trong hoac bao "SUCCESS" gia tao khi thuc ra chua lam gi ca (day la hanh dong nguoi dung
     * CHU Y bam, nen loi phai duoc bao ro, khac voi luc tu dong luc tao moi — xem
     * autoDiscoverOnCreate).
     */
    public Long resolveSitemapAndTrigger(Long competitorId, String triggeredBy) {
        Competitor competitor = competitorRepository.findById(competitorId).orElseThrow();
        if (!competitor.getExtractorConfig().containsKey(SITEMAP_URL_KEY)) {
            String failureReason = tryResolveSitemap(competitor);
            if (failureReason != null) {
                JobRun run = jobRunService.createQueued(JOB_KEY_PREFIX + competitorId, "MANUAL", triggeredBy);
                jobRunService.markFailed(run.getId(), failureReason);
                return run.getId();
            }
        }
        return trigger(competitorId, triggeredBy);
    }

    /**
     * Goi tu luc tao doi thu moi — im lang bo qua (khong tao JobRun) khi khong resolve duoc,
     * vi day la buoc "tien ich" tu dong, khong phai hanh dong nguoi dung chu y bam — tranh lam
     * phinh lich su job voi nhung lan "FAILED" ma nguoi dung khong thuc su yeu cau chay.
     */
    public void autoDiscoverOnCreate(Long competitorId, String triggeredBy) {
        Competitor competitor = competitorRepository.findById(competitorId).orElse(null);
        if (competitor == null) {
            return;
        }
        if (!competitor.getExtractorConfig().containsKey(SITEMAP_URL_KEY) && tryResolveSitemap(competitor) != null) {
            return;
        }
        try {
            trigger(competitorId, triggeredBy);
        } catch (Exception e) {
            log.warn("Khong kich hoat duoc discovery tu dong cho doi thu '{}': {}", competitor.getName(), e.getMessage());
        }
    }

    /**
     * Thu tim va luu sitemapUrl vao extractorConfig cua competitor (da la doi tuong quan ly
     * boi JPA trong pham vi method nay). Tra ve null neu thanh cong, hoac ly do loi (String)
     * neu khong resolve duoc — dung ly do nay de quyet dinh co bao FAILED ro rang hay khong,
     * tuy vao noi goi (xem 2 method public tren).
     */
    private String tryResolveSitemap(Competitor competitor) {
        if (!isDomainAllowedToCrawl(competitor.getBaseUrl())) {
            String reason = "Domain '" + competitor.getBaseUrl() + "' chua co trong allowed-crawl-domains "
                    + "(can quan tri vien them vao cau hinh server truoc)";
            log.info("Doi thu '{}': {}", competitor.getName(), reason);
            return reason;
        }
        String userAgent = appProperties.getSecurity().getCrawlUserAgent();
        Optional<String> resolved = sitemapUrlResolver.resolve(competitor.getBaseUrl(), userAgent);
        if (resolved.isEmpty()) {
            String reason = "Khong tu dong tim duoc sitemap san pham cho " + competitor.getBaseUrl();
            log.info("Doi thu '{}': {}", competitor.getName(), reason);
            return reason;
        }
        Map<String, Object> merged = new HashMap<>(competitor.getExtractorConfig());
        merged.put(SITEMAP_URL_KEY, resolved.get());
        competitor.setExtractorConfig(merged);
        competitorRepository.save(competitor);
        log.info("Tu dong tim duoc sitemap cho '{}': {}", competitor.getName(), resolved.get());
        return null;
    }

    private boolean isDomainAllowedToCrawl(String baseUrl) {
        try {
            String host = URI.create(baseUrl).getHost();
            if (host == null) {
                return false;
            }
            String lowerHost = host.toLowerCase(Locale.ROOT);
            return appProperties.getSecurity().getAllowedCrawlDomains().stream()
                    .map(d -> d.toLowerCase(Locale.ROOT))
                    .anyMatch(domain -> lowerHost.equals(domain) || lowerHost.endsWith("." + domain));
        } catch (Exception e) {
            return false;
        }
    }

    public static String jobKeyFor(Long competitorId) {
        return JOB_KEY_PREFIX + competitorId;
    }
}
