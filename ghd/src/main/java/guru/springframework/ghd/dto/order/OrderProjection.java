package guru.springframework.ghd.dto.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderProjection {
    String getId();
    String getCustomerName();
    String getCustomerPhone();
    BigDecimal getTotalAmount();
    String getPaymentMethod();
    String getStatus();
    LocalDateTime getCreatedDate();
    String getShippingAddress();
    String getNote();
}
