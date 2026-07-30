package guru.springframework.ghd.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ProductResponse {
    private String id;
    private String title;
    private String categoryName;
    private String brandName;
    private Integer status;
    private String image;
    private String priceRange;
    private Integer totalStock;
    private String description;

    public ProductResponse(String id, String title, String categoryName, String brandName,
                           Integer status, String image, String priceRange, Object totalStock, String description) {
        this.id = id;
        this.title = title;
        this.categoryName = categoryName;
        this.brandName = brandName;
        this.status = status;
        this.image = image;
        this.priceRange = priceRange;
        this.description = description;
        if (totalStock instanceof Number) {
            this.totalStock = ((Number) totalStock).intValue();
        } else {
            this.totalStock = 0;
        }
    }
}