package com.example.mcprice.service;

import com.example.mcprice.exception.ConflictException;
import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.repository.ProductRepository;
import com.example.mcprice.domain.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Quan ly vong doi cua competitor_listings: tao/cap nhat, phat hien conflict, duyet thu cong. */
@Service
@RequiredArgsConstructor
@Transactional
public class MatchingService {

    /** Cac trang thai coi la "dang thuc su gan" cho 1 san pham — REJECTED khong nam trong day,
     * de listing bi "Xoa" van co the duoc tim thay lai qua chuc nang "Tim theo SKU". */
    private static final List<MatchStatus> ACTIVELY_ASSIGNED_STATUSES =
            List.of(MatchStatus.REVIEW_REQUIRED, MatchStatus.AUTO_CONFIRMED, MatchStatus.MANUALLY_CONFIRMED);

    private final CompetitorListingRepository competitorListingRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final ProductRepository productRepository;
    private final CompetitorRepository competitorRepository;
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

    /**
     * Them thu cong 1 URL vao danh sach cua doi thu, dung khi nguoi dung tu tim thay URL dung
     * (vd qua chuc nang "Tim theo SKU") ma pipeline tu dong khong khop duoc hoac khop sai. Coi
     * nhu da xac nhan (MANUALLY_CONFIRMED) ngay vi nguoi dung da tu kiem tra URL truoc khi them.
     * Tu choi (conflict) neu URL nay DA CO trong danh sach cua doi thu — du la gan cho san pham
     * nao — de tranh tao ban ghi trung lap ma nguoi dung khong biet.
     */
    public CompetitorListingDto addManualListing(Long competitorId, Long productId, String url) {
        String trimmedUrl = url.trim();
        if (competitorListingRepository.findByCompetitorIdAndUrl(competitorId, trimmedUrl).isPresent()) {
            throw new ConflictException("URL '" + trimmedUrl + "' da co trong danh sach cua doi thu nay");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> NotFoundException.of("Product", productId));
        Competitor competitor = competitorRepository.findById(competitorId)
                .orElseThrow(() -> NotFoundException.of("Competitor", competitorId));
        CompetitorListing listing = CompetitorListing.builder()
                .product(product)
                .competitor(competitor)
                .url(trimmedUrl)
                .active(true)
                .matchMethod(MatchMethod.MANUAL)
                .matchScore(BigDecimal.ONE)
                .matchReason("Nguoi dung tu tim va them thu cong")
                .matchStatus(MatchStatus.MANUALLY_CONFIRMED)
                .build();
        CompetitorListing saved = competitorListingRepository.save(listing);
        auditService.record("MATCH_MANUAL_ADD", "COMPETITOR_LISTING", String.valueOf(saved.getId()),
                Map.of("productId", productId, "competitorId", competitorId, "url", trimmedUrl));
        return toDto(saved);
    }

    /**
     * Tim URL ung cu vien tren TOAN HE THONG theo SKU, dung cho chuc nang "Tim theo SKU" o trang
     * chi tiet san pham — loai tru nhung listing DA thuoc ve chinh san pham nay (khong can goi y
     * lai). Gioi han 20 ket qua de tranh tra ve qua nhieu neu SKU qua chung chung.
     */
    @Transactional(readOnly = true)
    public List<CompetitorListingDto> searchCandidatesBySku(Long productId, String sku) {
        return competitorListingRepository
                .findCandidatesBySkuExcludingProduct(productId, sku, ACTIVELY_ASSIGNED_STATUSES, PageRequest.of(0, 20))
                .stream().map(this::toDto).toList();
    }

    /**
     * Chuyen (gan lai) mot listing DANG THUOC san pham khac ve san pham hien tai — dung khi
     * nguoi dung tim thay qua "Tim theo SKU" nhung listing do dang bi gan nham cho san pham khac.
     * Coi nhu da xac nhan khop ngay (MANUALLY_CONFIRMED) vi nguoi dung da tu kiem tra truoc khi bam.
     */
    public CompetitorListingDto reassignListing(Long listingId, Long newProductId) {
        CompetitorListing listing = competitorListingRepository.findById(listingId)
                .orElseThrow(() -> NotFoundException.of("CompetitorListing", listingId));
        Product newProduct = productRepository.findById(newProductId)
                .orElseThrow(() -> NotFoundException.of("Product", newProductId));
        Long oldProductId = listing.getProduct().getId();
        listing.setProduct(newProduct);
        listing.setMatchMethod(MatchMethod.MANUAL);
        listing.setMatchScore(BigDecimal.ONE);
        listing.setMatchReason("Nguoi dung chuyen tu san pham #" + oldProductId + " qua tim kiem SKU");
        listing.setMatchStatus(MatchStatus.MANUALLY_CONFIRMED);
        listing.setActive(true);
        CompetitorListing saved = competitorListingRepository.save(listing);
        auditService.record("MATCH_REASSIGN", "COMPETITOR_LISTING", String.valueOf(listingId),
                Map.of("fromProductId", oldProductId, "toProductId", newProductId));
        return toDto(saved);
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

    /** Doc listing + map sang DTO trong CUNG mot transaction/session — tranh LazyInitializationException
     * khi entity duoc doc o mot method rieng (session da dong) roi moi truyen sang toDto() o day. */
    @Transactional(readOnly = true)
    public CompetitorListingDto getDto(Long listingId) {
        CompetitorListing listing = competitorListingRepository.findById(listingId)
                .orElseThrow(() -> NotFoundException.of("CompetitorListing", listingId));
        return toDto(listing);
    }

    /** Giong getDto nhung kiem tra them listing co thuoc dung productId khong (dung cho endpoint
     * long trong /api/products/{productId}/matches/{matchId}, tranh nguoi dung doan ID lay nham listing). */
    @Transactional(readOnly = true)
    public CompetitorListingDto getDtoOwnedByProduct(Long productId, Long listingId) {
        return toDto(getOwnedByProduct(productId, listingId));
    }

    public CompetitorListingDto toDto(CompetitorListing l) {
        PriceObservation latest = priceObservationRepository
                .findFirstByCompetitorListingIdOrderByCapturedAtDesc(l.getId()).orElse(null);
        return new CompetitorListingDto(l.getId(), l.getProduct().getId(), l.getProduct().getTitle(),
                l.getProduct().getSkuOriginal(), l.getCompetitor().getId(),
                l.getCompetitor().getName(), l.getUrl(), l.getExternalSku(), l.getMatchMethod().name(),
                l.getMatchScore(), l.getMatchReason(), l.getMatchStatus().name(), l.isActive(),
                latest == null ? null : latest.getPrice(),
                latest == null ? null : latest.getObservationStatus().name(),
                latest == null ? null : latest.getCapturedAt());
    }
}
