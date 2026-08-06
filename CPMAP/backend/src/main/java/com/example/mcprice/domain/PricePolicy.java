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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "price_policies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricePolicy extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyScope scope;

    @Column(length = 500)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder.Default
    @Column(name = "minimum_competitor_count", nullable = false)
    private int minimumCompetitorCount = 2;

    @Builder.Default
    @Column(name = "max_observation_age_hours", nullable = false)
    private int maxObservationAgeHours = 48;

    @Builder.Default
    @Column(name = "rounding_step", nullable = false, precision = 18, scale = 2)
    private BigDecimal roundingStep = BigDecimal.valueOf(10000);

    @Builder.Default
    @Column(name = "max_increase_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxIncreasePercent = BigDecimal.valueOf(15);

    @Builder.Default
    @Column(name = "max_decrease_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxDecreasePercent = BigDecimal.valueOf(15);

    @Builder.Default
    @Column(name = "outlier_threshold_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal outlierThresholdPercent = BigDecimal.valueOf(30);

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "outlier_strategy", nullable = false, length = 20)
    private OutlierStrategy outlierStrategy = OutlierStrategy.FLAG_ONLY;

    @Builder.Default
    @Column(name = "require_manual_approval", nullable = false)
    private boolean requireManualApproval = true;

    @Column(name = "minimum_allowed_price", precision = 18, scale = 2)
    private BigDecimal minimumAllowedPrice;

    @Column(name = "maximum_allowed_price", precision = 18, scale = 2)
    private BigDecimal maximumAllowedPrice;

    @Builder.Default
    @Column(name = "auto_publish_enabled", nullable = false)
    private boolean autoPublishEnabled = false;
}
