package com.example.mcprice.service;

import com.example.mcprice.domain.AuditLog;
import com.example.mcprice.repository.AuditLogRepository;
import com.example.mcprice.config.CorrelationIdFilter;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi audit log cho cac hanh dong nhay cam: sua match, nhap gia tay, thay doi policy,
 * approve/reject, publish. Chay tren transaction REQUIRES_NEW de log khong bi rollback
 * theo transaction nghiep vu chinh khi co loi xay ra sau do.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, String entityId, Map<String, Object> details) {
        String actor = currentActor();
        AuditLog log = AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details == null ? Map.of() : details)
                .correlationId(MDC.get(CorrelationIdFilter.MDC_KEY))
                .createdAt(OffsetDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "SYSTEM";
        }
        return auth.getName();
    }
}
