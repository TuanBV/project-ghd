package com.example.mcprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_listing_id", nullable = false)
    private CompetitorListing competitorListing;

    @Column(precision = 18, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    private String availability;

    @Column(name = "raw_price_text", length = 500)
    private String rawPriceText;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "observation_status", nullable = false, length = 20)
    private ObservationStatus observationStatus;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "final_url", length = 1000)
    private String finalUrl;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "verified_by")
    private String verifiedBy;

    @Column(length = 1000)
    private String note;

    @Builder.Default
    @Column(nullable = false)
    private boolean excluded = false;

    @Column(name = "exclusion_reason", length = 500)
    private String exclusionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (capturedAt == null) {
            capturedAt = OffsetDateTime.now();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public boolean isUsableForAverage() {
        return !excluded && observationStatus == ObservationStatus.VALID && price != null && price.signum() > 0;
    }
}
