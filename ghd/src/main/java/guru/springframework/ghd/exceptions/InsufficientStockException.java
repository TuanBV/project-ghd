package guru.springframework.ghd.exceptions;

// Tách riêng khỏi RuntimeException chung để PaymentServiceImpl.handleIpn phân biệt được
// "hết hàng lúc IPN xác nhận" (payment vẫn SUCCESS, chỉ order chuyển PAYMENT_FAILED) với
// các lỗi khác (mà sẽ phải rollback toàn bộ transaction).
public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
