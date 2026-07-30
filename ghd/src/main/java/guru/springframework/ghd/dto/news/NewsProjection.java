package guru.springframework.ghd.dto.news;

import java.time.LocalDateTime;

public interface NewsProjection {
    String getId();
    String getTitle();
    String getSlug();
    String getSummary();
    String getContent();
    String getThumbnail();
    String getCategoryId();
    String getCategoryName();
    String getBrandId();
    String getBrandName();
    String getStatus();
    String getPostType();
    Integer getViewCount();
    Boolean getIsFeatured();
    LocalDateTime getCreatedDate();
    String getMetaTitle();
    String getMetaKeyword();
    String getMetaDesc();

}