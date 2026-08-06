package com.example.mcprice.service;

import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.dto.MatchCandidate;
import com.example.mcprice.dto.MatchResult;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghep MOT url vao MOT transaction rieng (REQUIRES_NEW) de loi cua mot URL khong anh huong cac URL khac. */
@Service
@RequiredArgsConstructor
public class SitemapMatchExecutor {

    private static final Logger log = LoggerFactory.getLogger(SitemapMatchExecutor.class);

    public enum Outcome { AUTO_CONFIRMED, REVIEW_REQUIRED, CONFLICT, NO_MATCH }

    private final ProductMatchingService productMatchingService;
    private final MatchingService matchingService;
    private final ProductRepository productRepository;
    private final CompetitorRepository competitorRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Outcome matchAndUpsertOne(Long competitorId, String url) {
        MatchResult result = productMatchingService.match(new MatchCandidate(null, null, url));
        if (result.matchedProductId() == null) {
            return Outcome.NO_MATCH;
        }
        Product product = productRepository.findById(result.matchedProductId()).orElse(null);
        Competitor competitor = competitorRepository.findById(competitorId).orElse(null);
        if (product == null || competitor == null) {
            return Outcome.NO_MATCH;
        }
        var outcome = matchingService.upsertListing(product, competitor, url, null, result.method(), result.score(),
                result.reason(), result.status());
        if (outcome.conflict()) {
            log.info("Conflict khi discovery: {}", outcome.conflictMessage());
            return Outcome.CONFLICT;
        }
        return result.status() == MatchStatus.AUTO_CONFIRMED ? Outcome.AUTO_CONFIRMED : Outcome.REVIEW_REQUIRED;
    }
}
