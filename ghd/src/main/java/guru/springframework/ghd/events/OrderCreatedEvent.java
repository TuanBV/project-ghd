package guru.springframework.ghd.events;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEvent(
        String orderId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String shippingAddress,
        String paymentMethod,
        BigDecimal totalAmount,
        String note,
        List<OrderItemInfo> items
) {
    public record OrderItemInfo(String productTitle, Integer quantity, BigDecimal price) {
    }
}
