package guru.springframework.ghd.entities;

import guru.springframework.ghd.constants.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.*;

@Table(name = "orders")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Orders extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Integer version;

    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 15)
    private String customerPhone;

    @Column(length = 100)
    private String customerEmail;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('PENDING', 'CONFIRMED', 'SHIPPING', 'COMPLETED', 'CANCELLED', 'AWAITING_PAYMENT', 'PAYMENT_FAILED')")
    private OrderStatus status;

    @Column(columnDefinition = "TEXT")
    private String adminNote;
}