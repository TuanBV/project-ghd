-- Bật/tắt phương thức thanh toán (feature admin): 1 sys_param key = 1 giá trị
-- PaymentEnum, giá trị "true"/"false" - đọc/ghi qua API sys_param có sẵn
-- (GET /api/v1/sys-param, PUT /api/v1/sys-param), không tạo bảng riêng.
-- CARD/INSTALLMENT (2 nhánh online qua VNPay) mặc định TẮT vì chưa test xong với
-- sandbox VNPay thật (xem .claude/skills/vnpay-payment/SKILL.md) - COD/BANK_TRANSFER
-- mặc định BẬT như hành vi hiện tại của trang giỏ hàng.
INSERT INTO sys_param (param_key, param_value, param_name, group_code, description, del_flag, created_date, updated_date)
VALUES
    ('payment.cod.enabled', 'true', 'Thanh toán khi nhận hàng (COD)', 'PAYMENT', 'Bật/tắt phương thức COD trên trang giỏ hàng.', 0, NOW(6), NOW(6)),
    ('payment.bank_transfer.enabled', 'true', 'Chuyển khoản ngân hàng', 'PAYMENT', 'Bật/tắt phương thức chuyển khoản trên trang giỏ hàng.', 0, NOW(6), NOW(6)),
    ('payment.card.enabled', 'false', 'Thẻ ngân hàng (VNPay)', 'PAYMENT', 'Bật/tắt thanh toán thẻ qua VNPay - đang mặc định tắt vì chưa test xong với sandbox VNPay thật.', 0, NOW(6), NOW(6)),
    ('payment.installment.enabled', 'false', 'Trả góp (VNPay)', 'PAYMENT', 'Bật/tắt trả góp qua VNPay - đang mặc định tắt vì chưa test xong với sandbox VNPay thật.', 0, NOW(6), NOW(6));
