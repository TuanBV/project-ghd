package com.example.mcprice.repository;

import com.example.mcprice.domain.ImportIssue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportIssueRepository extends JpaRepository<ImportIssue, Long> {

    @Query(value = "select i from ImportIssue i left join fetch i.importRow where i.importRun.id = :importRunId",
            countQuery = "select count(i) from ImportIssue i where i.importRun.id = :importRunId")
    Page<ImportIssue> findByImportRunId(@Param("importRunId") Long importRunId, Pageable pageable);

    long countByImportRunId(Long importRunId);
}
