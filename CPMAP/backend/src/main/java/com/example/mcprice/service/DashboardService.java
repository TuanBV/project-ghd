package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.util.MoneyUtil;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.domain.CrawlItem;
import com.example.mcprice.repository.CrawlItemRepository;
import com.example.mcprice.dto.DashboardCompetitorHealthDto;
import com.example.mcprice.dto.DashboardPriceTrendsDto;
import com.example.mcprice.dto.DashboardSummaryDto;
import com.example.mcprice.domain.JobKeys;
import com.example.mcprice.domain.JobRun;
import com.example.mcprice.repository.JobRunRepository;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.MerchantSyncItem;
import com.example.mcprice.repository.MerchantSyncItemRepository;
import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.domain.PriceRecommendation;
import com.example.mcprice.domain.RecommendationStatus;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.PriceRecommendationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Tong hop KPI cho dashboard. Toan bo la truy van tong hop (COUNT/GROUP BY), khong load tung entity rieng le. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final List<MatchStatus> CONFIRMED_STATUSES = List.of(MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED);

    private final ProductRepository productRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceRecommendationRepository priceRecommendationRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final CrawlItemRepository crawlItemRepository;
    private final MerchantSyncItemRepository merchantSyncItemRepository;
    private final JobRunRepository jobRunRepository;
    private final CompetitorRepository competitorRepository;

    public DashboardSummaryDto getSummary() {
        long totalProducts = productRepository.count();
        long matched = competitorListingRepository.countDistinctProductsByMatchStatusIn(CONFIRMED_STATUSES);
        long conflict = competitorListingRepository.countDistinctProductsByMatchStatusIn(List.of(MatchStatus.REVIEW_REQUIRED));
        long unmatched = productRepository.countProductsWithoutAnyListing();

        List<PriceRecommendation> latest = priceRecommendationRepository.findLatestPerProduct();

        Map<String, Long> bySourceCount = new LinkedHashMap<>();
        bySourceCount.put("0", 0L);
        bySourceCount.put("1", 0L);
        bySourceCount.put("2", 0L);
        bySourceCount.put("3+", 0L);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (RecommendationStatus status : RecommendationStatus.values()) {
            byStatus.put(status.name(), 0L);
        }
        long increased = 0;
        long decreased = 0;
        long unchanged = 0;
        BigDecimal totalDiff = BigDecimal.ZERO;
        int diffCount = 0;

        for (PriceRecommendation r : latest) {
            String bucket = r.getIncludedSourceCount() >= 3 ? "3+" : String.valueOf(r.getIncludedSourceCount());
            bySourceCount.merge(bucket, 1L, Long::sum);
            byStatus.merge(r.getStatus().name(), 1L, Long::sum);

            if (r.getFinalSuggestedPrice() != null && r.getCurrentPrice() != null) {
                int cmp = r.getFinalSuggestedPrice().compareTo(r.getCurrentPrice());
                if (cmp > 0) {
                    increased++;
                } else if (cmp < 0) {
                    decreased++;
                } else {
                    unchanged++;
                }
                totalDiff = totalDiff.add(r.getFinalSuggestedPrice().subtract(r.getCurrentPrice()));
                diffCount++;
            }
        }
        long productsWithoutRecommendation = totalProducts - latest.size();
        bySourceCount.merge("0", productsWithoutRecommendation, Long::sum);
        byStatus.merge(RecommendationStatus.INSUFFICIENT_DATA.name(), productsWithoutRecommendation, Long::sum);

        BigDecimal avgDiff = diffCount == 0 ? BigDecimal.ZERO
                : totalDiff.divide(BigDecimal.valueOf(diffCount), 2, java.math.RoundingMode.HALF_UP);

        Map<String, Double> crawlSuccessRate = computeCrawlSuccessRate();
        long staleCount = priceObservationRepository.countByObservationStatus(ObservationStatus.STALE);

        DashboardSummaryDto.LastRunInfo lastRun = jobRunRepository.findByJobKeyIn(JobKeys.ALL).stream()
                .max(Comparator.comparing(JobRun::getCreatedAt))
                .map(this::toLastRunInfo).orElse(null);
        DashboardSummaryDto.LastRunInfo lastError = jobRunRepository.findByJobKeyIn(JobKeys.ALL).stream()
                .filter(j -> j.getStatus() == RunStatus.FAILED)
                .max(Comparator.comparing(JobRun::getCreatedAt))
                .map(this::toLastRunInfo).orElse(null);

        long mcSyncSuccess = merchantSyncItemRepository.countByStatus(MerchantSyncItem.Status.SUCCESS);
        long mcSyncFailed = merchantSyncItemRepository.countByStatus(MerchantSyncItem.Status.FAILED);

        return new DashboardSummaryDto(totalProducts, matched, unmatched, conflict, bySourceCount, byStatus,
                increased, decreased, unchanged, totalDiff, avgDiff, crawlSuccessRate, staleCount, lastRun, lastError,
                mcSyncSuccess, mcSyncFailed);
    }

    private Map<String, Double> computeCrawlSuccessRate() {
        Map<String, long[]> counters = new LinkedHashMap<>();
        for (Object[] row : crawlItemRepository.countByCompetitorAndStatus()) {
            String competitorName = (String) row[0];
            CrawlItem.Status status = (CrawlItem.Status) row[1];
            long count = (Long) row[2];
            long[] counter = counters.computeIfAbsent(competitorName, k -> new long[2]);
            counter[0] += count;
            if (status == CrawlItem.Status.SUCCESS) {
                counter[1] += count;
            }
        }
        Map<String, Double> result = new LinkedHashMap<>();
        counters.forEach((name, counter) -> result.put(name, counter[0] == 0 ? 0.0 : (double) counter[1] / counter[0]));
        return result;
    }

    private DashboardSummaryDto.LastRunInfo toLastRunInfo(JobRun run) {
        return new DashboardSummaryDto.LastRunInfo(run.getJobKey(), run.getStatus().name(),
                run.getFinishedAt() == null ? null : run.getFinishedAt().toString());
    }

    public DashboardPriceTrendsDto getPriceTrends() {
        List<PriceRecommendation> latest = priceRecommendationRepository.findLatestPerProduct();

        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("<-15%", 0L);
        distribution.put("-15%..-5%", 0L);
        distribution.put("-5%..0%", 0L);
        distribution.put("0%", 0L);
        distribution.put("0%..5%", 0L);
        distribution.put("5%..15%", 0L);
        distribution.put(">15%", 0L);

        Map<String, BigDecimal[]> categoryAgg = new LinkedHashMap<>();
        Map<String, Long> categoryMissingData = new LinkedHashMap<>();
        List<DashboardPriceTrendsDto.TopMover> movers = new java.util.ArrayList<>();

        for (PriceRecommendation r : latest) {
            Product product = r.getProduct();
            String category = product.getGoogleCategory() == null ? "(khong co category)" : product.getGoogleCategory();

            if (r.getStatus() == RecommendationStatus.INSUFFICIENT_DATA) {
                categoryMissingData.merge(category, 1L, Long::sum);
            }

            if (r.getCurrentPrice() != null && r.getFinalSuggestedPrice() != null) {
                BigDecimal[] agg = categoryAgg.computeIfAbsent(category, k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO });
                agg[0] = agg[0].add(r.getCurrentPrice());
                agg[1] = agg[1].add(r.getFinalSuggestedPrice());
                agg[2] = agg[2].add(BigDecimal.ONE);

                BigDecimal percentChange = MoneyUtil.percentChange(r.getCurrentPrice(), r.getFinalSuggestedPrice());
                if (percentChange != null) {
                    distribution.merge(bucketOf(percentChange), 1L, Long::sum);
                    movers.add(new DashboardPriceTrendsDto.TopMover(product.getId(), product.getTitle(),
                            r.getCurrentPrice(), r.getFinalSuggestedPrice(), percentChange));
                }
            }
        }

        Map<String, DashboardPriceTrendsDto.CategoryPrice> categoryPrices = new LinkedHashMap<>();
        categoryAgg.forEach((category, agg) -> {
            long count = agg[2].longValue();
            categoryPrices.put(category, new DashboardPriceTrendsDto.CategoryPrice(
                    agg[0].divide(agg[2], 2, java.math.RoundingMode.HALF_UP),
                    agg[1].divide(agg[2], 2, java.math.RoundingMode.HALF_UP), count));
        });

        List<DashboardPriceTrendsDto.TopMover> topIncreasing = movers.stream()
                .sorted(Comparator.comparing(DashboardPriceTrendsDto.TopMover::percentChange).reversed())
                .limit(10).toList();
        List<DashboardPriceTrendsDto.TopMover> topDecreasing = movers.stream()
                .sorted(Comparator.comparing(DashboardPriceTrendsDto.TopMover::percentChange))
                .limit(10).toList();

        return new DashboardPriceTrendsDto(distribution, categoryPrices, topIncreasing, topDecreasing, categoryMissingData);
    }

    private String bucketOf(BigDecimal percentChange) {
        double value = percentChange.doubleValue();
        if (value < -15) {
            return "<-15%";
        }
        if (value < -5) {
            return "-15%..-5%";
        }
        if (value < 0) {
            return "-5%..0%";
        }
        if (value == 0) {
            return "0%";
        }
        if (value <= 5) {
            return "0%..5%";
        }
        if (value <= 15) {
            return "5%..15%";
        }
        return ">15%";
    }

    public DashboardCompetitorHealthDto getCompetitorHealth() {
        Map<String, Double> crawlSuccessRate = computeCrawlSuccessRate();

        Map<String, Long> confirmedCount = new LinkedHashMap<>();
        for (Object[] row : competitorListingRepository.countConfirmedByCompetitor(CONFIRMED_STATUSES)) {
            confirmedCount.put((String) row[0], (Long) row[1]);
        }

        Map<String, Long> totalByDate = new LinkedHashMap<>();
        Map<String, Long> successByDate = new LinkedHashMap<>();
        for (Object[] row : crawlItemRepository.countByDateAndStatus()) {
            String date = (String) row[0];
            String status = row[1].toString();
            long count = ((Number) row[2]).longValue();
            totalByDate.merge(date, count, Long::sum);
            if ("SUCCESS".equals(status)) {
                successByDate.merge(date, count, Long::sum);
            }
        }
        Map<String, Double> dailyRate = new LinkedHashMap<>();
        totalByDate.forEach((date, total) -> dailyRate.put(date, total == 0 ? 0.0 : successByDate.getOrDefault(date, 0L) / (double) total));

        Map<String, String> lastErrorByCompetitor = new LinkedHashMap<>();
        for (Competitor competitor : competitorRepository.findAll()) {
            if (competitor.getLastErrorMessage() != null) {
                lastErrorByCompetitor.put(competitor.getName(), competitor.getLastErrorMessage());
            }
        }

        return new DashboardCompetitorHealthDto(crawlSuccessRate, confirmedCount, dailyRate, lastErrorByCompetitor);
    }
}
