package guru.springframework.ghd.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Table(name = "product_image")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductImage extends BaseEntity{
    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private String id;

    @Version
    private Integer version;

    @Column(length = 36, nullable = false)
    private String productId;

    @Column(nullable = false)
    private String imageUrl;

    @Builder.Default
    @Column(nullable = false)
    private Integer sortOrder = 0;
}