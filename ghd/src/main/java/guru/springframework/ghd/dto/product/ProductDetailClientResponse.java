package guru.springframework.ghd.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

import static guru.springframework.ghd.utils.CommonUtil.toSlug;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ProductDetailClientResponse {
    private String id;
    private String title;
    private String variantName;
    private String categoryName;
    private String categoryId;
    private String brandName;
    private String brandId;
    private String policyId;
    private Integer status;
    private String description;
    private String content;
    private String specification;
    private String productId;
    private String sku;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stockQty;
    private Integer soldCount;
    private String color;
    private String size;
    private String image;
    private String slug;
    private List<ProductImageResponse> listImages;
    private String groupId;

    public String getCategorySlug() { return toSlug(this.categoryName);}
    public String getBrandSlug() { return toSlug(this.categoryName);}
}