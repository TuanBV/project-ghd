package guru.springframework.ghd.listeners;

import guru.springframework.ghd.events.OrderCreatedEvent;
import guru.springframework.ghd.services.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static guru.springframework.ghd.config.KafkaTopicConfig.ORDER_EVENTS_TOPIC;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final TelegramService telegramService;

    @KafkaListener(topics = ORDER_EVENTS_TOPIC, groupId = "ghd-notifications")
    public void onOrderCreated(OrderCreatedEvent event) {
        telegramService.sendMessage(buildTelegramOrderMessage(event));
    }

    private String buildTelegramOrderMessage(OrderCreatedEvent event) {
        List<String> productLines = event.items().stream()
                .map(item -> "• " + escapeHtml(item.productTitle()) + " x " + item.quantity() + " - " + item.price())
                .toList();

        return """
            🛒 <b>CÓ ĐƠN HÀNG MỚI</b>

            <b>Mã đơn:</b> #%s
            <b>Khách hàng:</b> %s
            <b>SĐT:</b> %s
            <b>Email:</b> %s
            <b>Địa chỉ:</b> %s
            <b>Thanh toán:</b> %s
            <b>Tổng tiền:</b> %s

            <b>Sản phẩm:</b>
            %s

            <b>Ghi chú:</b> %s
            """.formatted(
                event.orderId(),
                escapeHtml(event.customerName()),
                escapeHtml(event.customerPhone()),
                escapeHtml(event.customerEmail()),
                escapeHtml(event.shippingAddress()),
                escapeHtml(event.paymentMethod()),
                escapeHtml(String.valueOf(event.totalAmount())),
                String.join("\n", productLines),
                escapeHtml(event.note())
        );
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
