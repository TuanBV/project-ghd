package com.example.mcprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mc_offer_id", unique = true)
    private String mcOfferId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "item_group_id")
    private String itemGroupId;

    @Column(name = "item_group_id_normalized")
    private String itemGroupIdNormalized;

    @Column(name = "sku_original")
    private String skuOriginal;

    @Column(name = "sku_normalized")
    private String skuNormalized;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "product_url", length = 1000)
    private String productUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    private String brand;

    @Column(name = "google_category", length = 500)
    private String googleCategory;

    @Column(name = "product_type", length = 500)
    private String productType;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String condition = "UNKNOWN";

    @Column(name = "raw_condition", length = 50)
    private String rawCondition;

    @Builder.Default
    @Column(nullable = false, length = 30)
    private String availability = "UNKNOWN";

    @Column(name = "raw_availability", length = 50)
    private String rawAvailability;

    @Column(name = "current_website_price", precision = 18, scale = 2)
    private BigDecimal currentWebsitePrice;

    @Column(name = "current_mc_price", precision = 18, scale = 2)
    private BigDecimal currentMcPrice;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "import_source", length = 50)
    private String importSource;

    @Column(name = "source_updated_at")
    private OffsetDateTime sourceUpdatedAt;
}
