package guru.springframework.ghd.dto.order;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String productId;
    private String productName;      // Tên sản phẩm chính
    private String sku;              // Mã SKU của biến thể
    private String color;            // Màu sắc
    private String size;             // Kích thước
    private String image;            // Hình ảnh sản phẩm tại thời điểm mua
    private Integer quantity;        // Số lượng mua
    private Double price;            // Giá bán tại thời điểm mua
    private Double totalItemAmount;  // Thành tiền của sản phẩm này (price * quantity)
}