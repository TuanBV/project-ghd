package guru.springframework.ghd.dto.order;

import guru.springframework.ghd.constants.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private String id;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shippingAddress;
    private String note;
    private String adminNote;
    private BigDecimal totalAmount;
    private String paymentMethod;
    // Null cho COD/BANK_TRANSFER (không có Payment). Xem Payment/PaymentStatus.
    private String paymentStatus;
    private String status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
