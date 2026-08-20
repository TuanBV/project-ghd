package guru.springframework.ghd.repositories;

import guru.springframework.ghd.constants.enums.OrderStatus;
import guru.springframework.ghd.entities.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, UUID> {

    @Query(value = "SELECT * FROM orders WHERE id = :orderId LIMIT 1", nativeQuery = true)
    Optional<Orders> findByOrderId(@Param("orderId") String orderId);

    // JPQL (không phải native) - Spring Data JPA hỗ trợ đầy đủ Pageable/Sort động ở đây,
    // khác findAllNative cũ (native query + Pageable/Sort tự chèn ORDER BY luôn 500,
    // cùng lớp bug với CategoryRepository.findAll). orderId/customerName cùng đến từ 1 ô
    // tìm kiếm trên admin/order.html nên OR với nhau (khớp 1 trong 2 là đủ); các filter
    // còn lại độc lập, AND với nhau.
    @Query("""
        SELECT o FROM Orders o
        WHERE (
                (:orderId IS NULL AND :customerName IS NULL)
                OR (:orderId IS NOT NULL AND o.id = :orderId)
                OR (:customerName IS NOT NULL AND o.customerName LIKE CONCAT('%', :customerName, '%'))
              )
          AND (:phone IS NULL OR o.customerPhone = :phone)
          AND (:status IS NULL OR o.status = :status)
          AND (:startDate IS NULL OR o.createdDate >= :startDate)
          AND (:endDate IS NULL OR o.createdDate <= :endDate)
          AND o.delFlag = 0
        """)
    Page<Orders> search(@Param("orderId") UUID orderId,
                         @Param("customerName") String customerName,
                         @Param("phone") String phone,
                         @Param("status") OrderStatus status,
                         @Param("startDate") LocalDateTime startDate,
                         @Param("endDate") LocalDateTime endDate,
                         Pageable pageable);

    // Khoá row Orders - dùng bởi PaymentServiceImpl.retryPayment làm mutex: 2 lần gọi
    // retry đồng thời cho CÙNG 1 order phải serialize qua khoá này (không chỉ khoá
    // Payment), để lần retry chạy sau luôn thấy và huỷ đúng payment PENDING mà lần
    // chạy trước vừa tạo, thay vì cả 2 cùng tạo payment PENDING song song.
    @Query(value = "SELECT * FROM orders WHERE id = :orderId FOR UPDATE", nativeQuery = true)
    Optional<Orders> findByOrderIdForUpdate(@Param("orderId") String orderId);
}
