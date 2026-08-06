package com.example.mcprice.controller;

import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.exception.NotFoundException;
import com.example.mcprice.domain.ImportRun;
import com.example.mcprice.dto.ImportIssueDto;
import com.example.mcprice.dto.ImportRunDto;
import com.example.mcprice.repository.ImportIssueRepository;
import com.example.mcprice.repository.ImportRunRepository;
import com.example.mcprice.service.ComparisonImportService;
import com.example.mcprice.service.ComparisonReportExportService;
import com.example.mcprice.service.McImportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final McImportService mcImportService;
    private final ComparisonImportService comparisonImportService;
    private final ComparisonReportExportService comparisonReportExportService;
    private final ImportRunRepository importRunRepository;
    private final ImportIssueRepository importIssueRepository;

    @PostMapping("/mc")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ImportRunDto importMc(@RequestParam("file") MultipartFile file) {
        try {
            return mcImportService.importFile(file.getOriginalFilename(), file.getInputStream(), currentActor());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @PostMapping("/comparison")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ImportRunDto importComparison(@RequestParam("file") MultipartFile file) {
        try {
            return comparisonImportService.importFile(file.getOriginalFilename(), file.getInputStream(), currentActor());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @GetMapping("/{id}")
    public ImportRunDto get(@PathVariable Long id) {
        ImportRun run = importRunRepository.findById(id).orElseThrow(() -> NotFoundException.of("ImportRun", id));
        return new ImportRunDto(run.getId(), run.getImportType().name(), run.getFileName(), run.getStatus().name(),
                run.getTotalRows(), run.getSuccessRows(), run.getIssueRows(), run.getStartedAt(), run.getFinishedAt());
    }

    @GetMapping("/{id}/issues")
    public PageResponse<ImportIssueDto> issues(@PathVariable Long id,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        var result = importIssueRepository.findByImportRunId(id, PageRequest.of(page, size))
                .map(issue -> new ImportIssueDto(issue.getId(),
                        issue.getImportRow() == null ? null : issue.getImportRow().getId(),
                        issue.getImportRow() == null ? null : issue.getImportRow().getRowNumber(),
                        issue.getIssueType(), issue.getSeverity().name(), issue.getMessage(), issue.isResolved()));
        return PageResponse.of(result);
    }

    @GetMapping("/comparison/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<byte[]> exportComparisonReport() {
        byte[] content = comparisonReportExportService.exportXlsx();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bao-cao-so-sanh-gia-chuan.xlsx\"")
                .body(content);
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }
}
