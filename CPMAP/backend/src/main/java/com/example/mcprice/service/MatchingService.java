package com.example.mcprice.service;

import com.example.mcprice.exception.ConflictException;
import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.Product;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quan ly vong doi cua competitor_listings: tao/cap nhat, phat hien conflict, duyet thu cong. */
@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    private final CompetitorListingRepository competitorListingRepository;
    private final AuditService auditService;

    public record UpsertOutcome(CompetitorListing listing, boolean conflict, String conflictMessage) {
    }

    /**
     * Tao hoac cap nhat mot competitor listing cho san pham. Neu URL nay da duoc gan cho
     * MOT san pham KHAC, day la conflict — khong duoc tu dong gan de, phai tra ve de nguoi
     * dung xu ly (khong ghi de, khong xoa ban ghi cu).
     */
    public UpsertOutcome upsertListing(Product product, Competitor competitor, String url, String externalSku,
                                        MatchMethod method, BigDecimal score, String reason, MatchStatus status) {
        Optional<CompetitorListing> existing = competitorListingRepository.findByCompetitorIdAndUrl(competitor.getId(), url);
        if (existing.isPresent() && !existing.get().getProduct().getId().equals(product.getId())) {
            String message = "URL '" + url + "' cua doi thu '" + competitor.getName() + "' da duoc gan cho san pham #"
                    + existing.get().getProduct().getId() + ", khong the gan tiep cho san pham #" + product.getId();
            return new UpsertOutcome(existing.get(), true, message);
        }
        CompetitorListing listing = existing.orElseGet(() -> CompetitorListing.builder()
                .product(product)
                .competitor(competitor)
                .url(url)
                .active(true)
                .build());
        listing.setExternalSku(externalSku);
        listing.setMatchMethod(method);
        listing.setMatchScore(score);
        listing.setMatchReason(reason);
        listing.setMatchStatus(status);
        CompetitorListing saved = competitorListingRepository.save(listing);
        return new UpsertOutcome(saved, false, null);
    }

    public CompetitorListingDto confirm(Long productId, Long listingId) {
        CompetitorListing listing = getOwnedByProduct(productId, listingId);
        listing.setMatchStatus(MatchStatus.MANUALLY_CONFIRMED);
        CompetitorListing saved = competitorListingRepository.save(listing);
        auditService.record("MATCH_CONFIRM", "COMPETITOR_LISTING", String.valueOf(listingId),
                Map.of("productId", productId, "competitor", saved.getCompetitor().getName()));
        return toDto(saved);
    }

    public CompetitorListingDto reject(Long productId, Long listingId, String reasonNote) {
        CompetitorListing listing = getOwnedByProduct(productId, listingId);
        listing.setMatchStatus(MatchStatus.REJECTED);
        listing.setActive(false);
        listing.setMatchReason(reasonNote == null || reasonNote.isBlank() ? listing.getMatchReason() : reasonNote);
        CompetitorListing saved = competitorListingRepository.save(listing);
        auditService.record("MATCH_REJECT", "COMPETITOR_LISTING", String.valueOf(listingId),
                Map.of("productId", productId, "reason", String.valueOf(reasonNote)));
        return toDto(saved);
    }

    private CompetitorListing getOwnedByProduct(Long productId, Long listingId) {
        CompetitorListing listing = competitorListingRepository.findById(listingId)
                .orElseThrow(() -> NotFoundException.of("CompetitorListing", listingId));
        if (!listing.getProduct().getId().equals(productId)) {
            throw new ConflictException("Match #" + listingId + " khong thuoc san pham #" + productId);
        }
        return listing;
    }

    public CompetitorListingDto toDto(CompetitorListing l) {
        return new CompetitorListingDto(l.getId(), l.getProduct().getId(), l.getProduct().getTitle(),
                l.getProduct().getSkuOriginal(), l.getCompetitor().getId(),
                l.getCompetitor().getName(), l.getUrl(), l.getExternalSku(), l.getMatchMethod().name(),
                l.getMatchScore(), l.getMatchReason(), l.getMatchStatus().name(), l.isActive());
    }
}
