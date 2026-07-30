package guru.springframework.ghd.dto.product;

import java.math.BigDecimal;

public interface IProductDetailClient {
    String getId();
    String getTitle();
    String getVariantName();
    String getDescription();
    String getCategoryName();
    String getCategoryId();
    String getBrandName();
    String getBrandId();
    String getPolicyId();
    Integer getStatus();
    String getContent();
    String getSpecification();
    String getSku();
    BigDecimal getPrice();
    BigDecimal getSalePrice();
    Integer getStockQty();
    String getColor();
    String getSize();
    String getImage();
    String getSlug();
    Integer getSoldCount();
    String getGroupId();
}
