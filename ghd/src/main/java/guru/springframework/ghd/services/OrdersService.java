package guru.springframework.ghd.services;

import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.dto.order.OrderCreationResult;
import guru.springframework.ghd.dto.order.OrderDetailResponse;
import guru.springframework.ghd.dto.order.OrderRequest;
import guru.springframework.ghd.dto.order.OrderResponse;
import guru.springframework.ghd.dto.order.OrderUpdateRequest;
import guru.springframework.ghd.entities.Orders;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

public interface OrdersService {

    /**
     * COD/BANK_TRANSFER: trừ kho ngay, {@code paymentUrl} null trong kết quả trả về.
     * CARD/INSTALLMENT: KHÔNG trừ kho, tạo {@code Payment} PENDING, trả về
     * {@code paymentUrl} để frontend redirect sang VNPay - xem
     * {@code guru.springframework.ghd.services.impl.PaymentServiceImpl} cho phần trừ
     * kho thật sự xảy ra lúc IPN xác nhận thành công.
     */
    OrderCreationResult createOrder(@Valid OrderRequest orderRequest, String clientIp);

    Page<OrderResponse> searchOrders(String orderId, String customerName, String phone, OrderStatus status, LocalDate startDate, LocalDate endDate, String sortField, String sortDir, int pageNumber, int sizeNumber);

    OrderDetailResponse findByOrderId(String orderId);

    void updateOrderStatus(String orderId, OrderUpdateRequest request);
}