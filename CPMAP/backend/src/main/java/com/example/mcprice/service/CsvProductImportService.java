package com.example.mcprice.service;

import com.example.mcprice.domain.ImportIssue;
import com.example.mcprice.domain.ImportIssueSeverity;
import com.example.mcprice.domain.ImportRow;
import com.example.mcprice.domain.ImportRowStatus;
import com.example.mcprice.domain.ImportRun;
import com.example.mcprice.domain.ImportType;
import com.example.mcprice.domain.Product;
import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.dto.ImportRunDto;
import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.repository.ImportIssueRepository;
import com.example.mcprice.repository.ImportRowRepository;
import com.example.mcprice.repository.ImportRunRepository;
import com.example.mcprice.repository.ProductRepository;
import com.example.mcprice.util.FileHashUtil;
import com.example.mcprice.util.SkuNormalizer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Import danh sach san pham co ban tu file CSV (cot: ID, SKU, Ten) — khac voi McImportService
 * (feed Google Merchant Center day du 17 cot). Dung khi chi can co ten + SKU truoc, cac truong
 * gia/danh muc/hinh anh se duoc bo sung sau qua cac buoc khac (khong xu ly o day).
 *
 * Idempotent theo SKU: neu SKU (chuan hoa) da ton tai trong DB, BO QUA khong tao lai/cap nhat —
 * chi tao moi cho SKU chua co, dung theo yeu cau nghiep vu (khong tu dong ghi de du lieu da co).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CsvProductImportService {

    private static final String HEADER_ID = "id";
    private static final String HEADER_SKU = "sku";
    private static final String HEADER_STOCK = "con hang";

    private final ImportRunRepository importRunRepository;
    private final ImportRowRepository importRowRepository;
    private final ImportIssueRepository importIssueRepository;
    private final ProductRepository productRepository;
    private final ProductAliasService productAliasService;
    private final AuditService auditService;

    public ImportRunDto importFile(String fileName, InputStream inputStream, String triggeredBy) {
        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Khong doc duoc file: " + e.getMessage());
        }
        String fileHash = FileHashUtil.sha256(content);

        var existing = importRunRepository.findByImportTypeAndFileHash(ImportType.CSV_PRODUCT, fileHash);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        ImportRun run = importRunRepository.save(ImportRun.builder()
                .importType(ImportType.CSV_PRODUCT)
                .fileName(fileName)
                .fileHash(fileHash)
                .status(RunStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .triggeredBy(triggeredBy)
                .build());

        List<String> lines = splitLines(content);
        processRows(run, lines);

        auditService.record("IMPORT_CSV_PRODUCT", "IMPORT_RUN", String.valueOf(run.getId()),
                Map.of("fileName", String.valueOf(fileName), "totalRows", run.getTotalRows(), "issueRows", run.getIssueRows()));
        return toDto(run);
    }

    private void processRows(ImportRun run, List<String> lines) {
        int headerIndex = findHeaderRowIndex(lines);
        Map<String, Integer> columnIndex = headerIndex < 0 ? defaultColumnIndex()
                : resolveColumnIndex(parseCsvLine(lines.get(headerIndex)));

        Map<String, Integer> skuFrequency = new HashMap<>();
        List<List<String>> dataRows = new ArrayList<>();
        for (int i = headerIndex + 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            List<String> fields = parseCsvLine(lines.get(i));
            dataRows.add(fields);
            String sku = valueAt(fields, columnIndex.get(HEADER_SKU));
            if (sku != null && !sku.isBlank()) {
                skuFrequency.merge(SkuNormalizer.normalize(sku), 1, Integer::sum);
            }
        }

        Map<String, Boolean> skuSeenInFile = new HashMap<>();
        int totalRows = 0;
        int successRows = 0;

        for (int i = 0; i < dataRows.size(); i++) {
            totalRows++;
            List<String> fields = dataRows.get(i);
            int rowNumber = headerIndex + i + 2;
            String externalId = valueAt(fields, columnIndex.get(HEADER_ID));
            String skuRaw = valueAt(fields, columnIndex.get(HEADER_SKU));
            String title = valueAt(fields, columnIndex.get("ten"));
            String stockRaw = valueAt(fields, columnIndex.get(HEADER_STOCK));
            String availability = resolveAvailability(stockRaw);
            String skuNormalized = skuRaw == null || skuRaw.isBlank() ? null : SkuNormalizer.normalize(skuRaw);

            Map<String, Object> rawRow = new HashMap<>();
            rawRow.put("id", externalId);
            rawRow.put("sku", skuRaw);
            rawRow.put("ten", title);
            rawRow.put("conHang", stockRaw);

            ImportRow importRow = importRowRepository.save(ImportRow.builder()
                    .importRun(run)
                    .rowNumber(rowNumber)
                    .rowIdentity(FileHashUtil.rowIdentity(String.valueOf(externalId), String.valueOf(skuRaw)))
                    .rawRowJson(rawRow)
                    .status(ImportRowStatus.IMPORTED)
                    .build());

            if (skuNormalized == null) {
                addIssue(run, importRow, "MISSING_SKU", ImportIssueSeverity.WARNING,
                        "Dong thieu SKU, khong the kiem tra trung/tao san pham, dong nay bi bo qua");
                importRow.setStatus(ImportRowStatus.SKIPPED);
                importRowRepository.save(importRow);
                continue;
            }

            boolean isDuplicateInFile = skuFrequency.getOrDefault(skuNormalized, 0) > 1;
            boolean isFirstOccurrence = skuSeenInFile.putIfAbsent(skuNormalized, true) == null;
            if (isDuplicateInFile && !isFirstOccurrence) {
                addIssue(run, importRow, "DUPLICATE_SKU_IN_FILE", ImportIssueSeverity.ERROR,
                        "SKU '" + skuRaw + "' xuat hien nhieu lan trong file, chi giu dong dau tien");
                importRow.setStatus(ImportRowStatus.SKIPPED);
                importRowRepository.save(importRow);
                continue;
            }

            boolean alreadyExists = !productRepository.findAllBySkuNormalized(skuNormalized).isEmpty();
            if (alreadyExists) {
                addIssue(run, importRow, "SKU_ALREADY_EXISTS", ImportIssueSeverity.INFO,
                        "SKU '" + skuRaw + "' da co trong danh sach san pham, khong tao lai");
                importRow.setStatus(ImportRowStatus.SKIPPED);
                importRowRepository.save(importRow);
                continue;
            }

            Product product = createProduct(externalId, skuRaw, skuNormalized, title, availability);
            importRow.setProduct(product);
            importRowRepository.save(importRow);
            successRows++;
        }

        run.setTotalRows(totalRows);
        run.setSuccessRows(successRows);
        run.setIssueRows((int) importIssueRepository.countByImportRunId(run.getId()));
        run.setStatus(run.getIssueRows() == 0 ? RunStatus.SUCCESS : RunStatus.PARTIAL_SUCCESS);
        run.setFinishedAt(OffsetDateTime.now());
        importRunRepository.save(run);
    }

    private Product createProduct(String externalId, String skuRaw, String skuNormalized, String title, String availability) {
        Product product = Product.builder()
                .externalId(externalId)
                .skuOriginal(skuRaw)
                .skuNormalized(skuNormalized)
                .title(title == null || title.isBlank() ? "(khong co ten)" : title)
                .condition("UNKNOWN")
                .availability(availability)
                .currency("VND")
                .active(true)
                .importSource("CSV_PRODUCT")
                .sourceUpdatedAt(OffsetDateTime.now())
                .build();
        Product saved = productRepository.save(product);
        productAliasService.generateFromSku(saved);
        return saved;
    }

    /**
     * Cot "Con hang?" trong file dung dang co/khong o dang so: "1" = con hang, "0" = het hang.
     * Neu file khong co cot nay (dinh dang cu) hoac gia tri khac 0/1, giu nguyen mac dinh
     * "UNKNOWN" nhu truoc day — khong doan mo ho khi du lieu khong ro rang.
     */
    private String resolveAvailability(String stockRaw) {
        if (stockRaw == null) {
            return "UNKNOWN";
        }
        String trimmed = stockRaw.trim();
        if (trimmed.equals("1")) {
            return "IN_STOCK";
        }
        if (trimmed.equals("0")) {
            return "OUT_OF_STOCK";
        }
        return "UNKNOWN";
    }

    private void addIssue(ImportRun run, ImportRow row, String type, ImportIssueSeverity severity, String message) {
        importIssueRepository.save(ImportIssue.builder()
                .importRun(run)
                .importRow(row)
                .issueType(type)
                .severity(severity)
                .message(message)
                .build());
    }

    /** File co the co dong tieu de nao do truoc dong header thuc su — tim dong dau tien co chua "sku" (khong phan biet hoa/thuong). */
    private int findHeaderRowIndex(List<String> lines) {
        for (int i = 0; i < Math.min(lines.size(), 5); i++) {
            String lower = lines.get(i).toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("sku")) {
                return i;
            }
        }
        return lines.isEmpty() ? -1 : 0;
    }

    private Map<String, Integer> resolveColumnIndex(List<String> headerFields) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerFields.size(); i++) {
            String key = normalizeHeaderKey(headerFields.get(i));
            index.put(key, i);
        }
        if (!index.containsKey(HEADER_SKU)) {
            return defaultColumnIndex();
        }
        return index;
    }

    private Map<String, Integer> defaultColumnIndex() {
        // Dinh dang mac dinh da biet: cot 0=ID, 1=SKU, 2=Ten, 3=Con hang?.
        Map<String, Integer> index = new HashMap<>();
        index.put(HEADER_ID, 0);
        index.put(HEADER_SKU, 1);
        index.put("ten", 2);
        index.put(HEADER_STOCK, 3);
        return index;
    }

    /** Bo dau tieng Viet + ha thuong + bo dau cau (vd "?") de so khop ten cot linh hoat
     * (vd "Tên" -> "ten", "Còn hàng?" -> "con hang"). */
    private String normalizeHeaderKey(String header) {
        String withoutBom = header.replace("﻿", "").trim();
        String decomposed = java.text.Normalizer.normalize(withoutBom, java.text.Normalizer.Form.NFD);
        String withoutDiacritics = decomposed.replaceAll("\\p{M}", "").toLowerCase(java.util.Locale.ROOT).replace("đ", "d");
        return withoutDiacritics.replaceAll("[^a-z0-9 ]", "").trim();
    }

    private String valueAt(List<String> fields, Integer index) {
        if (index == null || index < 0 || index >= fields.size()) {
            return null;
        }
        String value = fields.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private List<String> splitLines(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8).replace("﻿", "");
        return List.of(text.split("\\r?\\n"));
    }

    /** Parser CSV don gian, ho tro truong trong dau nhay kep chua dau phay va dau nhay kep lap doi ("") de escape. */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    private ImportRunDto toDto(ImportRun run) {
        return new ImportRunDto(run.getId(), run.getImportType().name(), run.getFileName(), run.getStatus().name(),
                run.getTotalRows(), run.getSuccessRows(), run.getIssueRows(), run.getStartedAt(), run.getFinishedAt());
    }
}
