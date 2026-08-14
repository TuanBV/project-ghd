package guru.springframework.ghd.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only pageview log - intentionally not a BaseEntity subclass since
 * delFlag/updatedDate don't apply to immutable analytics events.
 */
@Entity
@Table(name = "page_view")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageView {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "visitor_id", nullable = false, length = 64)
    private String visitorId;

    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "referrer_host", length = 255)
    private String referrerHost;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_type", nullable = false, length = 20)
    private String deviceType;

    @Column(length = 50)
    private String browser;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false, nullable = false)
    private LocalDateTime createdDate;
}
