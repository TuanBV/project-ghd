package guru.springframework.ghd.dto.product;

public interface ProductProjection {
    String getId();
    String getTitle();
    String getCategoryName();
    String getBrandName();
    Integer getStatus();
    String getImage();
    String getPriceRange();
    // SQL SUM/CAST thường trả về Long hoặc BigInteger
    Number getTotalStock();
    String getDescription();
}