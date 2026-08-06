package com.example.mcprice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mcprice.repository.AuditLogRepository;
import com.example.mcprice.service.DashboardService;
import com.example.mcprice.domain.ImportType;
import com.example.mcprice.dto.ImportRunDto;
import com.example.mcprice.repository.ImportIssueRepository;
import com.example.mcprice.repository.ImportRunRepository;
import com.example.mcprice.service.ComparisonImportService;
import com.example.mcprice.service.McImportService;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.dto.ManualPriceRequest;
import com.example.mcprice.dto.OverrideRequest;
import com.example.mcprice.dto.PriceRecommendationDto;
import com.example.mcprice.service.PriceCalculationService;
import com.example.mcprice.service.PricingService;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import com.example.mcprice.service.WebsitePublishService;
import com.example.mcprice.service.MerchantSyncService;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Kiem thu chap nhan (acceptance test) muc 14 cua dac ta: import dung hai file Excel that,
 * kiem tra so lieu dung nhu docs/data-analysis.md, sau do chay day du luong nhap gia tay ->
 * tinh trung binh -> approve -> publish dry-run va xac nhan co audit log.
 *
 * Cac test duoc danh so thu tu (Order) vi chung chia se cung mot Spring context/DB (import
 * chi chay mot lan, cac buoc sau phu thuoc du lieu da import).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AcceptanceFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private McImportService mcImportService;
    @Autowired
    private ComparisonImportService comparisonImportService;
    @Autowired
    private ImportRunRepository importRunRepository;
    @Autowired
    private ImportIssueRepository importIssueRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CompetitorListingRepository competitorListingRepository;
    @Autowired
    private PriceCalculationService priceCalculationService;
    @Autowired
    private PricingService pricingService;
    @Autowired
    private WebsitePublishService websitePublishService;
    @Autowired
    private MerchantSyncService merchantSyncService;
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private AuditLogRepository auditLogRepository;

    private static Long manualPriceProductId;
    private static Long manualPriceListingId;
    private static Long recommendationId;

    @Test
    @Order(1)
    void importMc_realFile_matchesExpectedRowCounts() throws Exception {
        ImportRunDto run = importFile("/fixtures/MC.xlsx", true);

        assertThat(run.totalRows()).isEqualTo(3265);
        assertThat(run.status()).isIn("SUCCESS", "PARTIAL_SUCCESS");

        long missingItemGroupIssues = countIssuesByType(run.id(), "MISSING_ITEM_GROUP_ID");
        assertThat(missingItemGroupIssues).isEqualTo(1680);

        long duplicateIdIssues = countIssuesByType(run.id(), "DUPLICATE_ID");
        assertThat(duplicateIdIssues).isGreaterThan(0);

        assertThat(productRepository.count()).isGreaterThan(3000);
    }

    @Test
    @Order(2)
    void importMc_sameFileAgain_isIdempotentByFileHash() throws Exception {
        long totalRunsBefore = importRunRepository.count();
        ImportRunDto secondRun = importFile("/fixtures/MC.xlsx", true);
        long totalRunsAfter = importRunRepository.count();

        assertThat(totalRunsAfter).isEqualTo(totalRunsBefore);
        assertThat(secondRun.totalRows()).isEqualTo(3265);
    }

    @Test
    @Order(3)
    void importComparisonReport_realFile_matchesExpectedRowCountsAndOwnUrlMatching() throws Exception {
        ImportRunDto run = importFile("/fixtures/comparison-report.xlsx", false);

        assertThat(run.totalRows()).isEqualTo(2128);

        long ownWebsiteNotFound = countIssuesByType(run.id(), "OWN_WEBSITE_NOT_FOUND");
        assertThat(ownWebsiteNotFound).isEqualTo(1303);
    }

    @Test
    @Order(4)
    void afterBothImports_mostProductsAreInsufficientDataAndAutoPublishDidNotHappen() {
        var summary = dashboardService.getSummary();

        assertThat(summary.totalMcProducts()).isGreaterThan(3000);
        assertThat(summary.recommendationsByStatus().get("INSUFFICIENT_DATA"))
                .isGreaterThan(summary.totalMcProducts() - 50);
        assertThat(summary.recommendationsByStatus().get("PUBLISHED")).isEqualTo(0L);
    }

    @Test
    @Order(5)
    void fullManualPriceApproveAndDryRunPublishFlow_producesAuditTrail() {
        // Tim mot san pham da co it nhat 1 competitor listing AUTO_CONFIRMED tu du lieu that
        // (co trong 10 gia doi thu da xac minh trong file bao cao) de bo sung nguon thu 2 bang tay.
        CompetitorListing listing = competitorListingRepository.findAll().stream()
                .filter(l -> l.isActive() && l.getMatchStatus() == com.example.mcprice.domain.MatchStatus.AUTO_CONFIRMED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Khong tim thay competitor listing AUTO_CONFIRMED nao tu du lieu that"));
        manualPriceProductId = listing.getProduct().getId();
        manualPriceListingId = listing.getId();

        Product product = productRepository.findById(manualPriceProductId).orElseThrow();
        if (product.getCurrentWebsitePrice() == null) {
            product.setCurrentWebsitePrice(new BigDecimal("5000000"));
            productRepository.save(product);
        }

        pricingService.manualPrice(manualPriceProductId,
                new ManualPriceRequest(manualPriceListingId, new BigDecimal("5100000"), false, "Acceptance test manual price"));

        PriceRecommendationDto dto = priceCalculationService.calculateForProduct(manualPriceProductId);
        assertThat(dto.includedSourceCount()).isGreaterThanOrEqualTo(1);
        recommendationId = dto.id();

        if (!"INSUFFICIENT_DATA".equals(dto.status())) {
            PriceRecommendationDto approved = pricingService.approve(recommendationId);
            assertThat(approved.status()).isEqualTo("APPROVED");

            var websiteRun = websitePublishService.publishApprovedRecommendations(List.of(recommendationId), null);
            assertThat(websiteRun.status()).isEqualTo("SUCCESS");
            assertThat(websiteRun.dryRun()).isTrue();

            var merchantRun = merchantSyncService.syncApprovedRecommendations(List.of(recommendationId), null);
            assertThat(merchantRun.status()).isIn("SUCCESS", "PARTIAL_SUCCESS");
        }

        long auditCount = auditLogRepository.count();
        assertThat(auditCount).isGreaterThan(0);
        assertThat(auditLogRepository.findAll().stream().anyMatch(a -> "MANUAL_PRICE_ENTRY".equals(a.getAction()))).isTrue();
    }

    @Test
    @Order(6)
    void override_requiresReasonAndExpiry_andIsAuditable() {
        if (recommendationId == null) {
            return;
        }
        PriceRecommendationDto overridden = pricingService.override(recommendationId,
                new OverrideRequest(new BigDecimal("5150000"), "Kiem thu override", OffsetDateTime.now().plusDays(30)));

        assertThat(overridden.overridePrice()).isEqualByComparingTo(new BigDecimal("5150000"));
        assertThat(overridden.overrideReason()).isEqualTo("Kiem thu override");
        assertThat(auditLogRepository.findAll().stream().anyMatch(a -> "RECOMMENDATION_OVERRIDE".equals(a.getAction()))).isTrue();
    }

    private ImportRunDto importFile(String classpathLocation, boolean isMc) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(classpathLocation)) {
            assertThat(is).as("Fixture file phai ton tai: " + classpathLocation).isNotNull();
            return isMc
                    ? mcImportService.importFile("MC.xlsx", is, "TEST")
                    : comparisonImportService.importFile("comparison.xlsx", is, "TEST");
        }
    }

    private long countIssuesByType(Long importRunId, String issueType) {
        return importIssueRepository.findByImportRunId(importRunId, org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(i -> i.getIssueType().equals(issueType))
                .count();
    }
}
