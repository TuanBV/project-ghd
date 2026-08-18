package guru.springframework.ghd.dto.order;

// paymentUrl null cho COD/BANK_TRANSFER (đơn tạo xong ngay); khác null cho CARD/
// INSTALLMENT - frontend phải redirect sang paymentUrl để khách thanh toán trên VNPay.
public record OrderCreationResult(String orderId, String paymentUrl) {
}
