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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "price_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "current_price", precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "raw_average_price", precision = 18, scale = 2)
    private BigDecimal rawAveragePrice;

    @Column(name = "rounded_price", precision = 18, scale = 2)
    private BigDecimal roundedPrice;

    @Column(name = "final_suggested_price", precision = 18, scale = 2)
    private BigDecimal finalSuggestedPrice;

    @Builder.Default
    @Column(name = "included_source_count", nullable = false)
    private int includedSourceCount = 0;

    @Builder.Default
    @Column(name = "excluded_source_count", nullable = false)
    private int excludedSourceCount = 0;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "calculation_snapshot", nullable = false)
    private Map<String, Object> calculationSnapshot = Map.of();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationStatus status;

    @Column(name = "override_price", precision = 18, scale = 2)
    private BigDecimal overridePrice;

    @Column(name = "override_by")
    private String overrideBy;

    @Column(name = "override_reason", length = 1000)
    private String overrideReason;

    @Column(name = "override_expires_at")
    private OffsetDateTime overrideExpiresAt;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejected_by")
    private String rejectedBy;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public boolean isApprovedForPublish() {
        return status == RecommendationStatus.APPROVED;
    }

    /** Gia thuc te se duoc dung de publish: override (neu con hieu luc) uu tien hon final_suggested_price. */
    public BigDecimal effectivePrice() {
        if (overridePrice != null && (overrideExpiresAt == null || overrideExpiresAt.isAfter(OffsetDateTime.now()))) {
            return overridePrice;
        }
        return finalSuggestedPrice;
    }
}
