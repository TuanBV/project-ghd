package com.example.mcprice.service;

import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.CrawlItem;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.Product;
import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.repository.CrawlRunRepository;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Diem vao khi nguoi dung bam "Xac nhan" 1 listing dang REVIEW_REQUIRED: crawl gia THAT ngay
 * cho DUY NHAT listing vua duoc xac nhan (dong bo, vi chi 1 URL nen nhanh — khac voi cac job
 * bulk khac trong he thong chay ngam), roi tinh lai recommendation cho san pham lien quan, de
 * nguoi dung thay ngay gia doi thu ma khong can bam them nut nao khac.
 *
 * KHONG danh dau @Transactional o class nay, cung ly do voi CrawlOrchestrationService: CrawlRun
 * phai duoc commit ngay truoc khi CrawlItemExecutor (REQUIRES_NEW) tham chieu toi no.
 */
@Service
@RequiredArgsConstructor
public class MatchConfirmPriceService {

    private static final Logger log = LoggerFactory.getLogger(MatchConfirmPriceService.class);

    /** Nguong % lech gia de tu dong xac nhan 1 listing moi kham pha — trung voi nguong canh bao
     * gia trung binh o trang chi tiet san pham, giu nhat quan 1 quy tac "10%" duy nhat trong he thong. */
    private static final BigDecimal AUTO_CONFIRM_PRICE_THRESHOLD_PERCENT = BigDecimal.valueOf(10);

    private final CrawlRunRepository crawlRunRepository;
    private final CrawlItemExecutor crawlItemExecutor;
    private final PriceCalculationService priceCalculationService;
    private final MatchingService matchingService;
    private final CompetitorListingRepository competitorListingRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    /**
     * Goi SAU KHI MatchingService.confirm(...) da tra ve va transaction cap nhat matchStatus da
     * commit. Viec xac nhan khop DA THANH CONG roi — buoc crawl+tinh lai gia o day chi la
     * "tien ich them", nen BAT LOI TOAN BO o day va luon tra ve mot DTO hop le, khong bao gio de
     * loi crawl/tinh gia bien thanh HTTP 500 lam nguoi dung tuong nham xac nhan khop bi that bai.
     */
    public CompetitorListingDto crawlPriceAndRecalculate(CompetitorListingDto confirmed) {
        try {
            crawlOneSync(confirmed.competitorId(), confirmed.id());
            try {
                priceCalculationService.calculateForProduct(confirmed.productId());
            } catch (Exception e) {
                log.warn("Khong tinh lai gia duoc sau khi xac nhan khop cho product #{}: {}", confirmed.productId(), e.getMessage());
            }
            return matchingService.getDto(confirmed.id());
        } catch (Exception e) {
            log.warn("Loi ngoai du kien khi crawl+tinh lai gia sau khi xac nhan khop listing #{}: {}", confirmed.id(), e.getMessage());
            return confirmed;
        }
    }

    /**
     * Goi ngay SAU KHI discovery (SitemapMatchExecutor.matchAndUpsertOne) da tao/cap nhat xong
     * 1 listing o trang thai REVIEW_REQUIRED VA transaction do da commit — crawl gia ngay de
     * kiem tra do hop ly: neu gia lech <= 10% so voi gia hien tai cua san pham thi COI NHU DA
     * XAC NHAN (AUTO_CONFIRMED) va tinh lai gia trung binh luon, khong bat nguoi dung phai vao
     * duyet tay tung URL mot. Neu KHONG lay duoc gia, san pham chua co gia de so sanh, hoac gia
     * lech qua nhieu (co the la khop nham san pham khac, hoac gia that su khac biet dang ke) —
     * GIU NGUYEN REVIEW_REQUIRED de nguoi dung tu kiem tra bang tay (gia da crawl duoc, neu co,
     * van duoc luu lai de nguoi dung tham khao luc duyet, khong bi bo phi).
     *
     * Tra ve true neu da tu dong xac nhan, false neu van giu REVIEW_REQUIRED (kem ly do trong log).
     */
    public boolean tryAutoConfirmByPricePlausibility(Long listingId) {
        try {
            CompetitorListingDto before = matchingService.getDto(listingId);
            if (!"REVIEW_REQUIRED".equals(before.matchStatus())) {
                return false;
            }

            boolean crawlSuccess = crawlOneSync(before.competitorId(), listingId);
            if (!crawlSuccess) {
                return false;
            }

            CompetitorListingDto afterCrawl = matchingService.getDto(listingId);
            if (afterCrawl.lastPrice() == null) {
                return false;
            }
            Product product = productRepository.findById(afterCrawl.productId()).orElse(null);
            if (product == null || product.getCurrentWebsitePrice() == null
                    || product.getCurrentWebsitePrice().signum() <= 0) {
                return false;
            }

            BigDecimal percentDiff = afterCrawl.lastPrice().subtract(product.getCurrentWebsitePrice())
                    .abs()
                    .divide(product.getCurrentWebsitePrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            if (percentDiff.compareTo(AUTO_CONFIRM_PRICE_THRESHOLD_PERCENT) > 0) {
                log.info("Khong tu xac nhan listing #{}: gia lech {}% (nguong 10%)", listingId, percentDiff);
                return false;
            }

            autoConfirm(listingId, percentDiff, afterCrawl.lastPrice(), product.getCurrentWebsitePrice());
            try {
                priceCalculationService.calculateForProduct(afterCrawl.productId());
            } catch (Exception e) {
                log.warn("Khong tinh lai gia duoc sau khi tu dong xac nhan listing #{}: {}", listingId, e.getMessage());
            }
            return true;
        } catch (Exception e) {
            log.warn("Loi ngoai du kien khi kiem tra tu dong xac nhan gia cho listing #{}: {}", listingId, e.getMessage());
            return false;
        }
    }

    private void autoConfirm(Long listingId, BigDecimal percentDiff, BigDecimal competitorPrice, BigDecimal currentPrice) {
        CompetitorListing listing = competitorListingRepository.findById(listingId).orElseThrow();
        listing.setMatchStatus(MatchStatus.AUTO_CONFIRMED);
        competitorListingRepository.save(listing);
        auditService.record("MATCH_AUTO_CONFIRM_BY_PRICE", "COMPETITOR_LISTING", String.valueOf(listingId),
                Map.of("percentDiff", percentDiff, "competitorPrice", competitorPrice, "currentPrice", currentPrice));
    }

    /** Crawl dong bo 1 listing, tu tao/cap nhat CrawlRun rieng. Tra ve true neu crawl thanh cong. */
    private boolean crawlOneSync(Long competitorId, Long listingId) {
        CrawlRun run = crawlRunRepository.save(CrawlRun.builder()
                .triggerType("MANUAL")
                .status(RunStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .build());

        CrawlItem.Status itemStatus;
        try {
            itemStatus = crawlItemExecutor.crawlOne(run, competitorId, listingId);
        } catch (Exception e) {
            log.warn("Khong crawl duoc gia cho listing #{}: {}", listingId, e.getMessage());
            itemStatus = CrawlItem.Status.FAILED;
        }
        boolean success = itemStatus == CrawlItem.Status.SUCCESS;
        run.setTotalItems(1);
        run.setSuccessItems(success ? 1 : 0);
        run.setFailedItems(success ? 0 : 1);
        run.setStatus(success ? RunStatus.SUCCESS : RunStatus.FAILED);
        run.setFinishedAt(OffsetDateTime.now());
        crawlRunRepository.save(run);
        return success;
    }
}
