package guru.springframework.ghd.entities;

import guru.springframework.ghd.constants.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

// 1 row / 1 lần thử thanh toán online (CARD/INSTALLMENT qua VNPay), tách khỏi Orders để
// COD/BANK_TRANSFER không đụng bảng này, và để giữ lại lịch sử mọi lần thử (kể cả thất
// bại) cho đối soát/audit sau này. Xem OrdersServiceImpl (tạo PENDING lúc checkout) và
// PaymentServiceImpl.handleIpn (cập nhật khi có IPN).
@Table(name = "payment")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Version
    private Integer version;

    @Column(nullable = false, length = 36)
    private String orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String provider = "VNPAY";

    // "CARD" hoặc "INSTALLMENT" - xem PaymentEnum
    @Column(nullable = false, length = 20)
    private String paymentMethod;

    // Mã tham chiếu GHD tự sinh khi tạo payment, gửi cho VNPay - là chốt chống trùng khi
    // xử lý IPN (xem PaymentRepository.findByTxnRefForUpdate).
    @Column(nullable = false, length = 34, unique = true)
    private String txnRef;

    // Điền từ IPN sau khi VNPay xác nhận (typical vnp_TransactionNo - xác nhận với tài
    // liệu VNPay thật khi có sandbox).
    @Column(length = 50)
    private String gatewayTransactionId;

    // Điền từ IPN - chỉ để hiển thị/thống kê cho admin, KHÔNG dùng cho logic nghiệp vụ.
    @Column(length = 20)
    private String bankCode;

    // Mã response gốc từ VNPay, lưu nguyên văn để audit (typical vnp_ResponseCode).
    @Column(length = 10)
    private String responseCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDateTime ipnReceivedAt;

    // Toàn bộ query string IPN gốc - phục vụ đối soát thủ công trước khi có Query API
    // tự động (phase 2, xem plan).
    @Column(columnDefinition = "TEXT")
    private String rawIpnPayload;
}
