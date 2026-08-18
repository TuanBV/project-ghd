-- Thêm 2 giá trị mới cho luồng thanh toán online (CARD/INSTALLMENT qua VNPay):
-- AWAITING_PAYMENT (chờ IPN, chưa trừ kho) và PAYMENT_FAILED (IPN báo lỗi/huỷ/sai chữ
-- ký/sai số tiền, hoặc hết hàng đúng lúc IPN xác nhận thành công). Chỉ thêm giá trị được
-- phép, không đổi dữ liệu hiện có - an toàn cho các row đang là PENDING/CONFIRMED/...
ALTER TABLE orders
    MODIFY COLUMN status ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED', 'AWAITING_PAYMENT', 'PAYMENT_FAILED') DEFAULT 'PENDING';
