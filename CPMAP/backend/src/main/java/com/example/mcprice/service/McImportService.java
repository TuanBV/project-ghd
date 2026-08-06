package com.example.mcprice.service;

import com.example.mcprice.domain.RunStatus;
import com.example.mcprice.exception.BusinessRuleException;
import com.example.mcprice.util.AvailabilityNormalizer;
import com.example.mcprice.util.ConditionNormalizer;
import com.example.mcprice.util.FileHashUtil;
import com.example.mcprice.util.PriceParser;
import com.example.mcprice.util.SkuNormalizer;
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
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.io.IOException;
import java.io.InputStream;
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
 * Import feed Google Merchant Center (MC.xlsx, sheet "Trang tinh1", 17 cot nghiep vu).
 * Khong tu dong gop cac dong co id/URL trung nhau — moi dong trung duoc danh dau import_issue
 * va chi dong dau tien duoc dung de tao/cap nhat Product.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class McImportService {

    private static final String SHEET_NAME = "Trang tính1";
    private static final List<String> EXPECTED_HEADERS = List.of(
            "id", "item_group_id", "tiêu đề", "mô tả", "liên kết", "tình trạng", "giá", "còn hàng",
            "liên kết hình ảnh", "gtin", "mpn", "nhãn hiệu", "danh mục sản phẩm của Google", "loại sản phẩm",
            "nhãn tùy chỉnh 0", "nhãn tùy chỉnh 1", "nhãn tùy chỉnh 2");

    private final ImportRunRepository importRunRepository;
    private final ImportRowRepository importRowRepository;
    private final ImportIssueRepository importIssueRepository;
    private final ProductRepository productRepository;
    private final ProductAliasService productAliasService;
    private final AuditService auditService;
    private final ExcelRowReader excelRowReader = new ExcelRowReader();

    public ImportRunDto importFile(String fileName, InputStream inputStream, String triggeredBy) {
        byte[] content;
        try {
            content = inputStream.readAllBytes();
        } catch (IOException e) {
            throw new BusinessRuleException("Khong doc duoc file: " + e.getMessage());
        }
        String fileHash = FileHashUtil.sha256(content);

        var existing = importRunRepository.findByImportTypeAndFileHash(ImportType.MC, fileHash);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        ImportRun run = importRunRepository.save(ImportRun.builder()
                .importType(ImportType.MC)
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
            List<String> headers = excelRowReader.readHeaders(sheet, 0, EXPECTED_HEADERS.size());
            processRows(run, sheet, headers);
        } catch (IOException e) {
            run.setStatus(RunStatus.FAILED);
            run.setFinishedAt(OffsetDateTime.now());
            importRunRepository.save(run);
            throw new BusinessRuleException("Loi doc file Excel MC: " + e.getMessage());
        }

        auditService.record("IMPORT_MC", "IMPORT_RUN", String.valueOf(run.getId()),
                Map.of("fileName", String.valueOf(fileName), "totalRows", run.getTotalRows(), "issueRows", run.getIssueRows()));
        return toDto(run);
    }

    private void processRows(ImportRun run, Sheet sheet, List<String> headers) {
        int lastRow = sheet.getLastRowNum();
        Map<String, Integer> idFrequency = new HashMap<>();
        Map<String, Integer> urlFrequency = new HashMap<>();
        Map<String, Integer> groupIdFrequency = new HashMap<>();

        List<Map<String, Object>> rawRows = new java.util.ArrayList<>();
        for (int r = 1; r <= lastRow; r++) {
            Map<String, Object> raw = excelRowReader.readRow(sheet, r, headers);
            if (excelRowReader.isRowBlank(raw)) {
                continue;
            }
            rawRows.add(raw);
            String id = ExcelValueUtil.asText(raw.get("id"));
            String url = ExcelValueUtil.asText(raw.get("liên kết"));
            String groupId = ExcelValueUtil.asText(raw.get("item_group_id"));
            if (id != null) {
                idFrequency.merge(id, 1, Integer::sum);
            }
            if (url != null) {
                urlFrequency.merge(url, 1, Integer::sum);
            }
            if (groupId != null) {
                groupIdFrequency.merge(SkuNormalizer.normalize(groupId), 1, Integer::sum);
            }
        }

        Map<String, Boolean> idSeen = new HashMap<>();
        Map<String, Boolean> urlSeen = new HashMap<>();
        int totalRows = 0;
        int successRows = 0;
        int issueRows = 0;

        for (int i = 0; i < rawRows.size(); i++) {
            totalRows++;
            Map<String, Object> raw = rawRows.get(i);
            int rowNumber = i + 2;
            String id = ExcelValueUtil.asText(raw.get("id"));
            String url = ExcelValueUtil.asText(raw.get("liên kết"));
            String groupId = ExcelValueUtil.asText(raw.get("item_group_id"));
            String rowIdentity = FileHashUtil.rowIdentity(String.valueOf(id), String.valueOf(url));

            ImportRow importRow = importRowRepository.save(ImportRow.builder()
                    .importRun(run)
                    .rowNumber(rowNumber)
                    .rowIdentity(rowIdentity)
                    .rawRowJson(new HashMap<>(raw))
                    .status(ImportRowStatus.IMPORTED)
                    .build());

            boolean isDuplicateId = id != null && idFrequency.get(id) > 1;
            boolean isDuplicateUrl = url != null && urlFrequency.get(url) > 1;
            boolean firstOfDuplicateId = isDuplicateId && idSeen.putIfAbsent(id, true) == null;
            boolean firstOfDuplicateUrl = isDuplicateUrl && urlSeen.putIfAbsent(url, true) == null;

            boolean skipProductUpsert = false;
            if (isDuplicateId && !firstOfDuplicateId) {
                addIssue(run, importRow, "DUPLICATE_ID", ImportIssueSeverity.ERROR,
                        "id '" + id + "' bi trung, dong nay bi bo qua (giu dong dau tien)");
                skipProductUpsert = true;
            }
            if (isDuplicateUrl && !firstOfDuplicateUrl) {
                addIssue(run, importRow, "DUPLICATE_URL", ImportIssueSeverity.ERROR,
                        "URL '" + url + "' bi trung, dong nay bi bo qua (giu dong dau tien)");
                skipProductUpsert = true;
            }
            if (groupId == null) {
                addIssue(run, importRow, "MISSING_ITEM_GROUP_ID", ImportIssueSeverity.WARNING,
                        "Dong thieu item_group_id, khong the dung lam SKU chinh");
            } else if (groupIdFrequency.get(SkuNormalizer.normalize(groupId)) > 1) {
                addIssue(run, importRow, "DUPLICATE_ITEM_GROUP_ID", ImportIssueSeverity.INFO,
                        "item_group_id '" + groupId + "' xuat hien o nhieu dong (co the la bien the cung model)");
            }

            if (skipProductUpsert) {
                importRow.setStatus(ImportRowStatus.SKIPPED);
                issueRows++;
            } else {
                Product product = upsertProduct(raw, id, url, groupId);
                importRow.setProduct(product);
                if (importRow.getStatus() == ImportRowStatus.IMPORTED) {
                    successRows++;
                }
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

    private Product upsertProduct(Map<String, Object> raw, String id, String url, String groupId) {
        String title = ExcelValueUtil.asText(raw.get("tiêu đề"));
        String description = ExcelValueUtil.asText(raw.get("mô tả"));
        String rawCondition = ExcelValueUtil.asText(raw.get("tình trạng"));
        String rawAvailability = ExcelValueUtil.asText(raw.get("còn hàng"));
        String priceText = ExcelValueUtil.asText(raw.get("giá"));
        String imageUrl = ExcelValueUtil.asText(raw.get("liên kết hình ảnh"));
        String brand = ExcelValueUtil.asText(raw.get("nhãn hiệu"));
        String googleCategory = ExcelValueUtil.asText(raw.get("danh mục sản phẩm của Google"));
        String productType = ExcelValueUtil.asText(raw.get("loại sản phẩm"));

        BigDecimal price = null;
        String currency = "VND";
        var parsed = PriceParser.parse(priceText);
        if (parsed.isPresent()) {
            price = parsed.get().amount();
            currency = parsed.get().currency();
        }

        Product product = (id == null ? java.util.Optional.<Product>empty() : productRepository.findByMcOfferId(id))
                .orElseGet(Product::new);
        product.setMcOfferId(id);
        product.setItemGroupId(groupId);
        product.setItemGroupIdNormalized(groupId == null ? null : SkuNormalizer.normalize(groupId));
        product.setSkuOriginal(groupId);
        product.setSkuNormalized(groupId == null ? null : SkuNormalizer.normalize(groupId));
        product.setTitle(title == null ? "(khong co tieu de)" : title);
        product.setDescription(description);
        product.setProductUrl(url);
        product.setImageUrl(imageUrl);
        product.setBrand(brand);
        product.setGoogleCategory(googleCategory);
        product.setProductType(productType);
        product.setCondition(ConditionNormalizer.normalize(rawCondition).name());
        product.setRawCondition(rawCondition);
        product.setAvailability(AvailabilityNormalizer.normalize(rawAvailability).name());
        product.setRawAvailability(rawAvailability);
        product.setCurrentMcPrice(price);
        product.setCurrency(currency);
        product.setActive(true);
        product.setImportSource("MC");
        product.setSourceUpdatedAt(OffsetDateTime.now());
        Product saved = productRepository.save(product);
        productAliasService.generateFromSku(saved);
        return saved;
    }

    private void addIssue(ImportRun run, ImportRow row, String type, ImportIssueSeverity severity, String message) {
        row.setStatus(ImportRowStatus.ISSUE);
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
