package com.example.mcprice.repository;

import com.example.mcprice.domain.ImportRun;
import com.example.mcprice.domain.ImportType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunRepository extends JpaRepository<ImportRun, Long> {
    Optional<ImportRun> findByImportTypeAndFileHash(ImportType importType, String fileHash);
}
