-- Lưu lại đúng giá trị vnp_CreateDate đã gửi lúc build URL thanh toán ban đầu, cần cho
-- Query API (querydr) đối soát sau này - vnp_TransactionDate phải khớp CHÍNH XÁC giá
-- trị gốc, không dùng payment.created_date vì có thể lệch vài trăm ms tới 1 giây (được
-- sinh ở 2 lời gọi LocalDateTime.now() khác nhau trong initiatePayment/buildPaymentUrl).
ALTER TABLE payment ADD COLUMN vnp_create_date VARCHAR(14) NULL;
