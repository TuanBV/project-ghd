package guru.springframework.ghd.services;

import guru.springframework.ghd.constants.enums.PaymentEnum;
import guru.springframework.ghd.dto.order.OrderCreationResult;
import guru.springframework.ghd.dto.payment.PaymentStatusResponse;
import guru.springframework.ghd.dto.payment.VnpayIpnResponse;
import guru.springframework.ghd.entities.Orders;

import java.util.Map;

public interface PaymentService {

    /**
     * Tạo 1 {@code Payment} PENDING cho đơn CARD/INSTALLMENT vừa tạo và trả về URL
     * redirect VNPay. Gọi từ {@code OrdersServiceImpl.createOrder} - order/OrderDetail
     * đã được persist trước khi gọi method này (xem plan).
     */
    String initiatePayment(Orders order, PaymentEnum paymentMethod, String clientIp);

    /**
     * Xử lý IPN từ VNPay (server-to-server) - nguồn sự thật DUY NHẤT cho việc đổi trạng
     * thái thanh toán/đơn hàng. Verify chữ ký, chống trùng, đối chiếu số tiền, trừ kho
     * (qua {@code StockService}) khi thành công, publish {@code OrderCreatedEvent} sau
     * commit - xem chi tiết trong impl và plan gốc của tính năng này.
     */
    VnpayIpnResponse handleIpn(Map<String, String> params);

    /**
     * Áp dụng kết quả gateway (đã verify chữ ký, hoặc đã chuẩn hoá từ
     * {@link VnpayService#queryTransaction} khi đối soát) cho payment ứng với
     * {@code txnRef} - CÙNG logic idempotency/đối chiếu số tiền/trừ kho với
     * {@link #handleIpn}, dùng chung để không viết trùng. Gọi bởi
     * {@code PaymentReconciliationServiceImpl} khi Query API xác nhận được kết quả
     * cuối cùng cho 1 payment PENDING mà IPN chưa từng tới.
     */
    VnpayIpnResponse applyGatewayResult(String txnRef, Map<String, String> gatewayParams);

    /**
     * Đọc (KHÔNG đổi) trạng thái thanh toán/đơn hàng hiện tại - dùng cho trang
     * return-URL poll hiển thị, không bao giờ dùng để mutate state.
     */
    PaymentStatusResponse getStatus(String orderId);

    /**
     * Như {@link #getStatus(String)} nhưng tra theo txnRef - dùng cho return URL, vì
     * VNPay redirect về kèm vnp_TxnRef chứ không phải orderId của GHD.
     */
    PaymentStatusResponse getStatusByTxnRef(String txnRef);

    /**
     * Tạo 1 lần thử thanh toán MỚI cho 1 đơn CARD/INSTALLMENT đang ở trạng thái
     * {@code PAYMENT_FAILED} hoặc {@code AWAITING_PAYMENT} (vd khách bị rớt mạng giữa
     * chừng, muốn thử lại) - reset đơn về {@code AWAITING_PAYMENT} rồi gọi lại
     * {@link #initiatePayment} như lúc tạo đơn lần đầu. Từ chối nếu đơn là COD/
     * BANK_TRANSFER hoặc đã ở trạng thái khác (đã thanh toán/đang xử lý xong rồi).
     */
    OrderCreationResult retryPayment(String orderId, String clientIp);
}
