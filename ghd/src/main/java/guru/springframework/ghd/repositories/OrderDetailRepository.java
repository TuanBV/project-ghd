package guru.springframework.ghd.repositories;

import guru.springframework.ghd.dto.order.OrderDetailProjection;
import guru.springframework.ghd.entities.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, UUID> {

    @Query(value = """
            SELECT 
                od.product_id AS productId,
                p.title AS productName,
                c.title AS categoryName,
                p.sku AS sku,
                p.color AS color,
                p.size AS size,
                p.image AS image,
                od.quantity AS quantity,
                od.price AS price,
                (od.quantity * od.price) AS totalItemAmount
            FROM order_detail od
            JOIN product p ON od.product_id = p.id
            JOIN category c ON c.id = p.category_id
            WHERE od.order_id = :orderId
            """, nativeQuery = true)
    List<OrderDetailProjection> findByOrderId(@Param("orderId") String orderId);
}