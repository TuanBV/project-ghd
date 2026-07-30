package guru.springframework.ghd.dto.product;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductVariantResponse {
    private String id;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stockQty;
    private String color;
    private String size;
    private String image;
    private String slug;
    private Integer soldCount;
}
