package guru.springframework.ghd.dto.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequest {

    private String id;

    private String tempId;

    @NotBlank(message = "Mã SKU không được để trống")
    @Size(max = 100, message = "SKU tối đa 100 ký tự")
    private String sku;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", message = "Giá không được âm")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal price;

    @NotNull(message = "Giá sale không được để trống")
    @DecimalMin(value = "0.0", message = "Giá sale không được âm")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal salePrice;

    @NotNull(message = "Số lượng tồn không được để trống")
    @Min(value = 0, message = "Số lượng tồn không được âm")
    private Integer stockQty;

    @Size(max = 50, message = "Màu sắc tối đa 50 ký tự")
    private String color;

    @Size(max = 50, message = "Kích thước tối đa 50 ký tự")
    private String size;

    @Size(max = 500, message = "Link ảnh quá dài")
    private String image;
}