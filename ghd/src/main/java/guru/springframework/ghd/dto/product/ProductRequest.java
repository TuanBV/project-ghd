package guru.springframework.ghd.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class ProductRequest {
    private String id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String title;
    private String variantName;

    private String categoryId;
    private String brandId;
    private String policyId;

    private String description;
    private String content;
    private String specification;

    private Integer status;
    private String sku;

    @Min(value = 0, message = "Giá không được nhỏ hơn 0")
    private Double price;

    private Double salePrice;

    @Min(value = 0, message = "Số lượng tồn kho không hợp lệ")
    private Integer stockQty;

    private String color;
    private String size;

    private List<String> removedImages;
    private List<String> productGroup;

    private String mainImage;
    private List<GalleryOrderItem> galleryOrder;
    private List<String> newGalleryFileIds;

    private Boolean duplicate;
    private String duplicateSourceId;
    private String existingImageMain;
}