package guru.springframework.ghd.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailResponse {
    private String id;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String note;
    private String adminNote;
    private BigDecimal totalAmount;
    private String paymentMethod;
    // Null cho COD/BANK_TRANSFER (không có Payment). Set thủ công trong
    // OrdersServiceImpl.findByOrderId (không qua OrderMapper vì Payment không có trên
    // entity Orders).
    private String paymentStatus;
    private String status;
    private List<OrderDetailProjection> orderItems;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
