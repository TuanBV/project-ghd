package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.dto.order.OrderCreationResult;
import guru.springframework.ghd.dto.order.OrderRequest;
import guru.springframework.ghd.dto.order.OrderResponse;
import guru.springframework.ghd.dto.order.OrderUpdateRequest;
import guru.springframework.ghd.entities.Orders;
import guru.springframework.ghd.services.OrdersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order")
public class OrderController extends BaseController {

    private final OrdersService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest orderRequest, HttpServletRequest servletRequest) {
        try
        {
            // CARD/INSTALLMENT trả về paymentUrl để frontend redirect sang VNPay; COD/
            // BANK_TRANSFER trả về paymentUrl=null như trước (không đổi hành vi).
            OrderCreationResult result = orderService.createOrder(orderRequest, servletRequest.getRemoteAddr());
            return ok(result);
        } catch (Exception ex) {
            return ng(ex.getMessage());
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable String orderId) {
        // API để xem lại đơn hàng sau khi đặt thành công
        return ok(orderService.findByOrderId(orderId));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<?> updateOrder(
            @PathVariable String orderId,
            @RequestBody OrderUpdateRequest request) {
        orderService.updateOrderStatus(orderId, request);

        return ok(null);
    }
    @GetMapping
    public ResponseEntity<?> searchOrders(
            @RequestParam(name = "ma-don-hang", required = false) String orderId,
            @RequestParam(name = "ten-khach-hang", required = false) String customerName,
            @RequestParam(name = "so-dien-thoai", required = false) String phone,
            @RequestParam(name = "trang-thai", required = false) OrderStatus status, // Sử dụng Enum OrderStatus đã tạo
            @RequestParam(name = "ngay-bat-dau", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "ngay-ket-thuc", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "trang", defaultValue = DefaultPage.PAGE_STRING) int pageNumber,
            @RequestParam(name = "so-ban-ghi", defaultValue = DefaultPage.SIZE_STRING) int sizeNumber,
            @RequestParam(name = "thuoc-tinh", defaultValue = DefaultPage.CREATED_DATE) String sortField,
            @RequestParam(name = "kieu-sap-xep", defaultValue = DefaultPage.DESC) String sortDir
    ) {
        // Gọi service xử lý logic tìm kiếm đơn hàng
        Page<OrderResponse> orderPage = orderService.searchOrders(
                orderId, customerName, phone, status, startDate, endDate, sortField, sortDir, pageNumber, sizeNumber
        );
        return ResponseEntity.ok(orderPage);
    }
}