package guru.springframework.ghd.repositories;

import guru.springframework.ghd.dto.order.OrderProjection;
import guru.springframework.ghd.entities.Orders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, UUID> {

    @Query(value = "SELECT * FROM orders WHERE id = :orderId LIMIT 1", nativeQuery = true)
    Optional<Orders> findByOrderId(@Param("orderId") String orderId);

    @Query(value = """
        SELECT 
            o.id AS id,
            o.customer_name AS customerName,
            o.customer_phone AS customerPhone,
            o.total_amount AS totalAmount,
            o.payment_method AS paymentMethod,
            o.status AS status,
            o.created_date AS createdDate,
            o.shipping_address AS shippingAddress,
            o.note AS note
        FROM orders o
        WHERE (:customerName IS NULL OR o.customer_name LIKE CONCAT('%', :customerName, '%'))
          AND (:phone IS NULL OR o.customer_phone = :phone)
          AND (:status IS NULL OR o.status = :status)
          AND (:startDate IS NULL OR o.created_date >= :startDate)
          AND (:endDate IS NULL OR o.created_date <= :endDate)
          AND o.del_flag = 0
        """,
            countQuery = """
        SELECT COUNT(*) FROM orders o
        WHERE (:customerName IS NULL OR o.customer_name LIKE CONCAT('%', :customerName, '%'))
          AND (:phone IS NULL OR o.customer_phone = :phone)
          AND (:status IS NULL OR o.status = :status)
          AND (:startDate IS NULL OR o.created_date >= :startDate)
          AND (:endDate IS NULL OR o.created_date <= :endDate)
          AND o.del_flag = 0
        """,
            nativeQuery = true)
    Page<OrderProjection> findAllNative(
            @Param("customerName") String customerName,
            @Param("phone") String phone,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}