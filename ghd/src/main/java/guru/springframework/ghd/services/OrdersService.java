package guru.springframework.ghd.services;

import guru.springframework.ghd.constants.enums.OrderStatus;
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

    void createOrder(@Valid OrderRequest orderRequest);

    Page<OrderResponse> searchOrders(String orderId, String customerName, String phone, OrderStatus status, LocalDate startDate, LocalDate endDate, String sortField, String sortDir, int pageNumber, int sizeNumber);

    OrderDetailResponse findByOrderId(String orderId);

    void updateOrderStatus(String orderId, OrderUpdateRequest request);
}