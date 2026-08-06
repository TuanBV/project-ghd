package com.example.mcprice.controller;

import com.example.mcprice.service.MerchantFeedExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/merchant")
@RequiredArgsConstructor
public class MerchantExportController {

    private final MerchantFeedExportService merchantFeedExportService;

    @GetMapping("/feed-export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ResponseEntity<byte[]> exportFeed() {
        byte[] content = merchantFeedExportService.exportXlsx();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mc-feed-export.xlsx\"")
                .body(content);
    }
}
