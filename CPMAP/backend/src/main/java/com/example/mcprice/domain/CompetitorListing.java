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
@Table(name = "competitor_listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompetitorListing extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_id", nullable = false)
    private Competitor competitor;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(name = "external_sku")
    private String externalSku;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_method", nullable = false, length = 30)
    private MatchMethod matchMethod;

    @Builder.Default
    @Column(name = "match_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal matchScore = BigDecimal.ZERO;

    @Column(name = "match_reason", length = 1000)
    private String matchReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_status", nullable = false, length = 20)
    private MatchStatus matchStatus;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public boolean isPublishReady() {
        return matchStatus == MatchStatus.AUTO_CONFIRMED || matchStatus == MatchStatus.MANUALLY_CONFIRMED;
    }
}
