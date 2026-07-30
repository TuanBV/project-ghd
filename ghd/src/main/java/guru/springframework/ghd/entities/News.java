package guru.springframework.ghd.entities;

import guru.springframework.ghd.constants.enums.NewsStatus;
import guru.springframework.ghd.constants.enums.PostType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Table(name = "news")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class News extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Integer version;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(length = 500)
    private String summary;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String thumbnail;

    @Column
    private String categoryId;

    @Column
    private String brandId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NewsStatus status = NewsStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PostType postType = PostType.NEWS;

    @Column
    private String metaTitle;

    @Column
    private String metaKeyword;

    @Column(length = 500)
    private String metaDesc;

    @Column
    private Integer viewCount = 0;

    @Column
    private Boolean isFeatured = false;

    @Column
    private String authorId;
}