package com.example.mcprice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_key", nullable = false, length = 100)
    private String jobKey;

    @Column(name = "trigger_type", nullable = false, length = 20)
    private String triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RunStatus status;

    @Builder.Default
    @Column(name = "total_items", nullable = false)
    private int totalItems = 0;

    @Builder.Default
    @Column(name = "success_items", nullable = false)
    private int successItems = 0;

    @Builder.Default
    @Column(name = "failed_items", nullable = false)
    private int failedItems = 0;

    @Builder.Default
    @Column(name = "progress_percent", nullable = false)
    private int progressPercent = 0;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "error_detail", columnDefinition = "text")
    private String errorDetail;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "triggered_by")
    private String triggeredBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
