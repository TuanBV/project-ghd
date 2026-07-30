package guru.springframework.ghd.dto.product;

import java.math.BigDecimal;

public interface ProductDetailCartResponse {
    String getProductId();
    String getCategoryName();
    String getTitle();
    String getDescription();
    BigDecimal getPrice();
    BigDecimal getSalePrice();
    String getImage();
    String getSlug();
}