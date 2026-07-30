package guru.springframework.ghd.dto.news;

import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewsResponse {
    private UUID id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String thumbnail;

    private String categoryId;
    private String categoryName;

    private String brandId;
    private String brandName;

    private String status;
    private String postType;

    // SEO
    private String metaTitle;
    private String metaKeyword;
    private String metaDesc;

    // Stats & Audit
    private Integer viewCount;
    private Boolean isFeatured;
    private String authorId;

    private LocalDateTime createdDate;
}