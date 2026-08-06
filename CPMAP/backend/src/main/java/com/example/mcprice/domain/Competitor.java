package com.example.mcprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "competitors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competitor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "base_url", nullable = false, length = 500)
    private String baseUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "crawl_mode", nullable = false, length = 20)
    private CrawlMode crawlMode;

    @Builder.Default
    @Column(name = "requests_per_minute", nullable = false)
    private int requestsPerMinute = 10;

    @Builder.Default
    @Column(name = "timeout_seconds", nullable = false)
    private int timeoutSeconds = 10;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extractor_config", nullable = false)
    private Map<String, Object> extractorConfig = Map.of();

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    @Column(name = "last_error_at")
    private OffsetDateTime lastErrorAt;

    @Column(name = "last_error_message", length = 1000)
    private String lastErrorMessage;

    /**
     * Tong so URL san pham lay duoc tu sitemap (sau khi loc bo file sitemap khong phai san
     * pham theo ten file — blog/brand/category/...) trong lan quet gan nhat. Day la TOAN BO
     * catalog san pham cua doi thu tren site, KHONG can doi chieu voi SKU/ten san pham cua
     * minh — doi chieu SKU la mot buoc khac, tach rieng (xem SitemapMatchExecutor), chi anh
     * huong so luong trong competitor_listings, khong anh huong den cot nay.
     */
    @Builder.Default
    @Column(name = "last_sitemap_url_count", nullable = false)
    private int lastSitemapUrlCount = 0;
}
