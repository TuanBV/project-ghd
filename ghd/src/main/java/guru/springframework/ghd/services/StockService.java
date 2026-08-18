package guru.springframework.ghd.services;

import guru.springframework.ghd.events.OrderCreatedEvent;

import java.util.List;

public interface StockService {

    /**
     * Trừ stockQty/tăng soldCount cho từng {@code OrderDetail} đã persist của 1 đơn
     * hàng, dùng {@code SELECT ... FOR UPDATE} để chống race giữa các checkout đồng
     * thời (cùng pattern với logic gốc trong {@code OrdersServiceImpl}).
     * <p>
     * Dùng chung cho cả nhánh COD/BANK_TRANSFER (lúc tạo đơn) và nhánh CARD/INSTALLMENT
     * (lúc IPN xác nhận thành công) - không viết trùng logic khoá pessimistic ở 2 nơi.
     *
     * @throws guru.springframework.ghd.exceptions.InsufficientStockException nếu bất kỳ
     *         dòng nào không đủ hàng - KHÔNG có dòng nào bị trừ (transaction của caller
     *         rollback toàn bộ khi exception này thoát ra khỏi method @Transactional).
     */
    List<OrderCreatedEvent.OrderItemInfo> decrementStockForOrder(String orderId);
}
