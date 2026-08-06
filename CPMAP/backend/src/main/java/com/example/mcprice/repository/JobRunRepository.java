package com.example.mcprice.repository;

import com.example.mcprice.domain.JobRun;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRunRepository extends JpaRepository<JobRun, Long> {

    Page<JobRun> findByJobKeyOrderByCreatedAtDesc(String jobKey, Pageable pageable);

    Optional<JobRun> findFirstByJobKeyOrderByCreatedAtDesc(String jobKey);

    List<JobRun> findByJobKeyIn(List<String> jobKeys);
}
