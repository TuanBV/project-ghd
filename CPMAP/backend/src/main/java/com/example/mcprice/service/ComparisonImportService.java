package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.util.FileHashUtil;
import com.example.mcprice.domain.Competitor;
import com.example.mcprice.repository.CompetitorRepository;
import com.example.mcprice.domain.ImportIssue;
import com.example.mcprice.domain.ImportIssueSeverity;
import com.example.mcprice.domain.ImportRow;
import com.example.mcprice.domain.ImportRowStatus;
import com.example.mcprice.domain.ImportRun;
import com.example.mcprice.domain.ImportType;
import com.example.mcprice.dto.ImportRunDto;
import com.example.mcprice.repository.ImportIssueRepository;
import com.example.mcprice.repository.ImportRowRepository;
import com.example.mcprice.repository.ImportRunRepository;
import com.example.mcprice.domain.MatchMethod;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.domain.ObservationStatus;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.domain.SourceType;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Import file bao cao so sanh gia (Bao_cao_so_sanh_gia_*.xlsx, sheet "So sanh gia").
 * Ghep voi Product da import tu MC bang URL - Tong kho <-> lien ket; tao/cap nhat
 * competitor_listings + price_observations tu 4 cot doi thu. Giá Min chi luu de audit,
 * KHONG dung de tinh gia de xuat.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ComparisonImportService {

    private static final String SHEET_NAME = "So sánh giá";
    private static final int COLUMN_COUNT = 19;

    private record CompetitorColumn(String name, String priceKey, String statusKey, String urlKey) {
    }

    private static final List<CompetitorColumn> COMPETITOR_COLUMNS = List.of(
            new CompetitorColumn("sgt.com.vn", "sgt.com.vn", "Trạng thái - SGT", "URL - SGT"),
            new CompetitorColumn("dienmay88.vn", "dienmay88.vn", "Trạng thái - Điện Máy 88", "URL - Điện Máy 88"),
            new CompetitorColumn("dienmaythienphu.vn", "dienmaythienphu.vn", "Trạng thái - Thiên Phú", "URL - Thiên Phú"),
            new CompetitorColumn("dienmayabc.com", "dienmayabc.com", "Trạng thái - Điện Máy ABC", "URL - Điện Máy ABC"));

    private final ImportRunRepository importRunRepository;
    private final ImportRowRepository importRowRepository;
    private final ImportIssueRepository importIssueRepository;
    private final ProductRepository productRepository;
    private final CompetitorRepository competitorRepository;
    private final PriceObservationRepository priceObservationRepository;
    private final MatchingService matchingService;
    private final AuditService auditService;
    private final ExcelRowReader excelRowReader = new ExcelRowReader();

    public ImportRunDto importFile(String fileName, java.io.InputStream inputStream, String triggeredBy) {
        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Khong doc duoc file: " + e.getMessage());
        }
        String fileHash = FileHashUtil.sha256(content);

        var existing = importRunRepository.findByImportTypeAndFileHash(ImportType.COMPARISON, fileHash);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        ImportRun run = importRunRepository.save(ImportRun.builder()
                .importType(ImportType.COMPARISON)
                .fileName(fileName)
                .fileHash(fileHash)
                .status(RunStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .triggeredBy(triggeredBy)
                .build());

        try (Workbook workbook = WorkbookFactory.create(new java.io.ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }
            List<String> headers = excelRowReader.readHeaders(sheet, 0, COLUMN_COUNT);
            processRows(run, sheet, headers);
        } catch (IOException e) {
            run.setStatus(RunStatus.FAILED);
            run.setFinishedAt(OffsetDateTime.now());
            importRunRepository.save(run);
            throw new BusinessRuleException("Loi doc file Excel bao cao so sanh gia: " + e.getMessage());
        }

        auditService.record("IMPORT_COMPARISON", "IMPORT_RUN", String.valueOf(run.getId()),
                Map.of("fileName", String.valueOf(fileName), "totalRows", run.getTotalRows(), "issueRows", run.getIssueRows()));
        return toDto(run);
    }

    private void processRows(ImportRun run, Sheet sheet, List<String> headers) {
        int lastRow = sheet.getLastRowNum();
        Map<String, Boolean> ownUrlSeen = new HashMap<>();
        Map<String, Integer> ownUrlFrequency = new HashMap<>();

        List<Map<String, Object>> rawRows = new java.util.ArrayList<>();
        for (int r = 1; r <= lastRow; r++) {
            Map<String, Object> raw = excelRowReader.readRow(sheet, r, headers);
            if (excelRowReader.isRowBlank(raw)) {
                continue;
            }
            rawRows.add(raw);
            String ownUrl = ExcelValueUtil.asText(raw.get("URL - Tổng kho"));
            if (ownUrl != null) {
                ownUrlFrequency.merge(ownUrl, 1, Integer::sum);
            }
        }

        int totalRows = 0;
        int successRows = 0;

        for (int i = 0; i < rawRows.size(); i++) {
            totalRows++;
            Map<String, Object> raw = rawRows.get(i);
            int rowNumber = i + 2;
            String sku = ExcelValueUtil.asText(raw.get("Mã sản phẩm / SKU"));
            String ownUrl = ExcelValueUtil.asText(raw.get("URL - Tổng kho"));
            String rowIdentity = FileHashUtil.rowIdentity(String.valueOf(sku), String.valueOf(ownUrl));

            ImportRow importRow = importRowRepository.save(ImportRow.builder()
                    .importRun(run)
                    .rowNumber(rowNumber)
                    .rowIdentity(rowIdentity)
                    .rawRowJson(new HashMap<>(raw))
                    .status(ImportRowStatus.IMPORTED)
                    .build());

            boolean hasIssue = false;
            Product product = null;
            if (ownUrl != null) {
                boolean isDuplicate = ownUrlFrequency.get(ownUrl) > 1;
                boolean isFirst = !isDuplicate || ownUrlSeen.putIfAbsent(ownUrl, true) == null;
                if (isDuplicate && !isFirst) {
                    addIssue(run, importRow, "DUPLICATE_URL_CONFLICT", ImportIssueSeverity.ERROR,
                            "URL '" + ownUrl + "' xuat hien o nhieu dong trong file bao cao, chi dong dau tien duoc dung de cap nhat gia web");
                    hasIssue = true;
                } else {
                    product = productRepository.findByProductUrl(ownUrl).orElse(null);
                    if (product == null) {
                        addIssue(run, importRow, "URL_NOT_IN_MC", ImportIssueSeverity.INFO,
                                "URL '" + ownUrl + "' chua co trong feed MC da import, khong the cap nhat gia web cho SKU '" + sku + "'");
                        hasIssue = true;
                    } else {
                        applyOwnWebsitePrice(raw, product);
                        importRow.setProduct(product);
                    }
                }
            } else {
                addIssue(run, importRow, "OWN_WEBSITE_NOT_FOUND", ImportIssueSeverity.INFO,
                        "SKU '" + sku + "' khong tim thay tren website cua minh (Trang thai - Tong kho: "
                                + ExcelValueUtil.asText(raw.get("Trạng thái - Tổng kho")) + ")");
                hasIssue = true;
            }

            if (product != null) {
                processCompetitorColumns(run, importRow, raw, product);
            }

            if (hasIssue) {
                importRow.setStatus(ImportRowStatus.ISSUE);
            } else {
                successRows++;
            }
            importRowRepository.save(importRow);
        }

        run.setTotalRows(totalRows);
        run.setSuccessRows(successRows);
        run.setIssueRows((int) importIssueRepository.countByImportRunId(run.getId()));
        run.setStatus(run.getIssueRows() == 0 ? RunStatus.SUCCESS : RunStatus.PARTIAL_SUCCESS);
        run.setFinishedAt(OffsetDateTime.now());
        importRunRepository.save(run);
    }

    private void applyOwnWebsitePrice(Map<String, Object> raw, Product product) {
        BigDecimal ownPrice = ExcelValueUtil.asNumber(raw.get("tongkhodienmaymienbac.com"));
        if (ownPrice != null && ownPrice.signum() > 0) {
            product.setCurrentWebsitePrice(ownPrice);
            productRepository.save(product);
        }
    }

    private void processCompetitorColumns(ImportRun run, ImportRow importRow, Map<String, Object> raw, Product product) {
        for (CompetitorColumn column : COMPETITOR_COLUMNS) {
            String url = ExcelValueUtil.asText(raw.get(column.urlKey()));
            if (url == null) {
                continue;
            }
            String statusText = ExcelValueUtil.asText(raw.get(column.statusKey()));
            MatchStatus matchStatus = mapMatchStatus(statusText);
            if (matchStatus == null) {
                continue;
            }
            Competitor competitor = competitorRepository.findByNameIgnoreCase(column.name()).orElse(null);
            if (competitor == null) {
                continue;
            }
            var outcome = matchingService.upsertListing(product, competitor, url, null, MatchMethod.IMPORTED_MAPPING,
                    BigDecimal.ONE, "Import tu bao cao so sanh gia, trang thai goc: " + statusText, matchStatus);
            if (outcome.conflict()) {
                addIssue(run, importRow, "DUPLICATE_URL_CONFLICT", ImportIssueSeverity.ERROR, outcome.conflictMessage());
                continue;
            }
            BigDecimal price = ExcelValueUtil.asNumber(raw.get(column.priceKey()));
            saveCompetitorObservation(outcome.listing(), price, statusText);
        }
    }

    private void saveCompetitorObservation(com.example.mcprice.domain.CompetitorListing listing, BigDecimal price,
                                            String statusText) {
        ObservationStatus status;
        if (price != null && price.signum() > 0) {
            status = ObservationStatus.VALID;
        } else {
            status = ObservationStatus.NO_PRICE;
        }
        priceObservationRepository.save(PriceObservation.builder()
                .competitorListing(listing)
                .price(status == ObservationStatus.VALID ? price : null)
                .currency("VND")
                .sourceType(SourceType.IMPORT)
                .observationStatus(status)
                .rawPriceText(price == null ? statusText : price.toPlainString())
                .capturedAt(OffsetDateTime.now())
                .note("Import tu bao cao so sanh gia")
                .build());
    }

    private MatchStatus mapMatchStatus(String statusText) {
        if (statusText == null) {
            return null;
        }
        return switch (statusText) {
            case "Khớp chính xác", "Khớp, không có giá" -> MatchStatus.AUTO_CONFIRMED;
            case "Nghi ngờ" -> MatchStatus.REVIEW_REQUIRED;
            default -> null;
        };
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

    private ImportRunDto toDto(ImportRun run) {
        return new ImportRunDto(run.getId(), run.getImportType().name(), run.getFileName(), run.getStatus().name(),
                run.getTotalRows(), run.getSuccessRows(), run.getIssueRows(), run.getStartedAt(), run.getFinishedAt());
    }
}
