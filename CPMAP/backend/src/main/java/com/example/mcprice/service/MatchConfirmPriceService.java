package com.example.mcprice.service;

import com.example.mcprice.domain.CrawlItem;
import com.example.mcprice.domain.CrawlRun;
import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CrawlRunRepository;
import java.time.OffsetDateTime;
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

    private final CrawlRunRepository crawlRunRepository;
    private final CrawlItemExecutor crawlItemExecutor;
    private final PriceCalculationService priceCalculationService;
    private final MatchingService matchingService;

    /**
     * Goi SAU KHI MatchingService.confirm(...) da tra ve va transaction cap nhat matchStatus da
     * commit. Viec xac nhan khop DA THANH CONG roi — buoc crawl+tinh lai gia o day chi la
     * "tien ich them", nen BAT LOI TOAN BO o day va luon tra ve mot DTO hop le, khong bao gio de
     * loi crawl/tinh gia bien thanh HTTP 500 lam nguoi dung tuong nham xac nhan khop bi that bai.
     */
    public CompetitorListingDto crawlPriceAndRecalculate(CompetitorListingDto confirmed) {
        try {
            CrawlRun run = crawlRunRepository.save(CrawlRun.builder()
                    .triggerType("MANUAL")
                    .status(RunStatus.RUNNING)
                    .startedAt(OffsetDateTime.now())
                    .build());

            CrawlItem.Status itemStatus;
            try {
                itemStatus = crawlItemExecutor.crawlOne(run, confirmed.competitorId(), confirmed.id());
            } catch (Exception e) {
                log.warn("Khong crawl duoc gia ngay sau khi xac nhan khop cho listing #{}: {}", confirmed.id(), e.getMessage());
                itemStatus = CrawlItem.Status.FAILED;
            }
            boolean success = itemStatus == CrawlItem.Status.SUCCESS;
            run.setTotalItems(1);
            run.setSuccessItems(success ? 1 : 0);
            run.setFailedItems(success ? 0 : 1);
            run.setStatus(success ? RunStatus.SUCCESS : RunStatus.FAILED);
            run.setFinishedAt(OffsetDateTime.now());
            crawlRunRepository.save(run);

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
}
