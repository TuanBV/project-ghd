package guru.springframework.ghd.constants.enums;

public enum OrderStatus {
    PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED,
    // Đơn thanh toán online (CARD/INSTALLMENT qua VNPay) đã tạo nhưng chưa có xác nhận
    // IPN - stock CHƯA bị trừ ở trạng thái này (xem OrdersServiceImpl.createOrder).
    AWAITING_PAYMENT,
    // IPN báo thất bại/huỷ, sai chữ ký, sai số tiền, hoặc hết hàng đúng lúc IPN xác nhận
    // thành công (tiền đã thu, cần hoàn tay - xem PaymentServiceImpl.handleIpn).
    PAYMENT_FAILED
}