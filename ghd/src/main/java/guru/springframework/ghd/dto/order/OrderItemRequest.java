package guru.springframework.ghd.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemRequest {
    @NotBlank(message = "ID sản phẩm không được trống")
    private String productId;

    @NotNull(message = "Số lượng không được trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    @NotNull(message = "Giá sản phẩm không được trống")
    private BigDecimal price;
}