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
@Table(name = "website_publish_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebsitePublishRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_run_id")
    private Long jobRunId;

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
    @Column(name = "dry_run", nullable = false)
    private boolean dryRun = true;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
