package guru.springframework.ghd.dto.product;

import guru.springframework.ghd.dto.policy.ProductSummaryDTO;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ProductDetailResponse {
    private String id;
    private String title;
    private String variantName;
    private String categoryId;
    private String brandId;
    private String policyId;
    private Integer status;
    private String content;
    private String description;
    private String specification;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stockQty;
    private String color;
    private String size;
    private String image;
    private String slug;
    private Integer soldCount;
    private List<ProductImageResponse> listImages;
    private List<ProductSummaryDTO> productGroup;

}