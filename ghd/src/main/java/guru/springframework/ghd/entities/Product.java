package guru.springframework.ghd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.*;

@Table(name = "product")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity{
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private String id;

    @Version
    private Integer version;

    @Column(length = 36, nullable = false)
    private String categoryId;

    @Column(length = 36, nullable = false)
    private String brandId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String specification;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 36, nullable = false)
    private String policyId;

    private Integer status;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal salePrice;

    private Integer stockQty;

    @Builder.Default
    @Column(nullable = false)
    private Integer soldCount = 0;

    @Column(name = "variant_name", length = 255)
    private String variantName;

    private String color;
    private String size;
    private String image;
    private String slug;

    private String groupId;
}