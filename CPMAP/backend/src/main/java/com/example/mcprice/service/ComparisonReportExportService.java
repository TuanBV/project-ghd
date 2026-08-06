package com.example.mcprice.service;

import com.example.mcprice.util.FormulaInjectionGuard;
import com.example.mcprice.domain.CompetitorListing;
import com.example.mcprice.domain.MatchStatus;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.domain.PriceObservation;
import com.example.mcprice.repository.PriceObservationRepository;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Xuat lai file "Bao cao so sanh gia" theo dung 19 cot cua schema goc, nhung dien bang du lieu
 * THAT hien co trong he thong (URL/gia da duoc kham pha qua sitemap + crawl that), thay vi
 * cac gia tri "Chua xac minh" ban dau. Day la "file chuan" theo dung nghia: moi con so deu
 * co the truy vet lai qua competitor_listings/price_observations, khong phai gia lap.
 */
@Service
@RequiredArgsConstructor
public class ComparisonReportExportService {

    private static final List<String> HEADERS = List.of(
            "Tên sản phẩm", "Mã sản phẩm / SKU", "Giá Min", "tongkhodienmaymienbac.com",
            "sgt.com.vn", "dienmay88.vn", "dienmaythienphu.vn", "dienmayabc.com",
            "Trạng thái - Tổng kho", "Trạng thái - SGT", "Trạng thái - Điện Máy 88",
            "Trạng thái - Thiên Phú", "Trạng thái - Điện Máy ABC",
            "URL - Tổng kho", "URL - SGT", "URL - Điện Máy 88", "URL - Thiên Phú", "URL - Điện Máy ABC",
            "Chênh lệch Tổng kho - Min");

    private static final List<String> COMPETITOR_NAMES = List.of(
            "sgt.com.vn", "dienmay88.vn", "dienmaythienphu.vn", "dienmayabc.com");

    private final ProductRepository productRepository;
    private final CompetitorListingRepository competitorListingRepository;
    private final PriceObservationRepository priceObservationRepository;

    @Transactional(readOnly = true)
    public byte[] exportXlsx() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("So sánh giá");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                headerRow.createCell(i).setCellValue(HEADERS.get(i));
            }

            int rowIndex = 1;
            for (Product product : productRepository.findAll()) {
                List<CompetitorListing> listings = competitorListingRepository.findByProductId(product.getId());
                Map<String, CompetitorListing> byCompetitor = new LinkedHashMap<>();
                for (CompetitorListing listing : listings) {
                    if (listing.isActive()) {
                        byCompetitor.put(listing.getCompetitor().getName(), listing);
                    }
                }
                // Chi xuat san pham co it nhat mot nguon du lieu (website minh hoac 1 doi thu tro len)
                // de file khong bi phinh to voi hang nghin dong hoan toan trong.
                if (product.getCurrentWebsitePrice() == null && byCompetitor.isEmpty()) {
                    continue;
                }
                writeRow(sheet.createRow(rowIndex++), product, byCompetitor);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeRow(Row row, Product product, Map<String, CompetitorListing> byCompetitor) {
        row.createCell(0).setCellValue(safe(product.getTitle()));
        row.createCell(1).setCellValue(safe(product.getSkuOriginal()));
        row.createCell(2).setCellValue(""); // Gia Min: chi mang tinh tham khao lich su, khong tinh lai o day
        setPrice(row.createCell(3), product.getCurrentWebsitePrice());

        int priceColStart = 4;
        int statusColStart = 8;
        int urlColStart = 13;
        for (int i = 0; i < COMPETITOR_NAMES.size(); i++) {
            CompetitorListing listing = byCompetitor.get(COMPETITOR_NAMES.get(i));
            BigDecimal latestPrice = latestValidPrice(listing);
            setPrice(row.createCell(priceColStart + i), latestPrice);
            row.createCell(statusColStart + 1 + i).setCellValue(statusLabel(listing));
            row.createCell(urlColStart + 1 + i).setCellValue(safe(listing == null ? null : listing.getUrl()));
        }

        row.createCell(statusColStart).setCellValue(product.getCurrentWebsitePrice() != null ? "Khớp chính xác" : "Không tìm thấy");
        row.createCell(urlColStart).setCellValue(safe(product.getProductUrl()));

        if (product.getCurrentWebsitePrice() != null) {
            BigDecimal min = minCompetitorPrice(byCompetitor);
            if (min != null) {
                row.createCell(18).setCellValue(product.getCurrentWebsitePrice().subtract(min).doubleValue());
            }
        }
    }

    private BigDecimal minCompetitorPrice(Map<String, CompetitorListing> byCompetitor) {
        return byCompetitor.values().stream()
                .map(this::latestValidPrice)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }

    private BigDecimal latestValidPrice(CompetitorListing listing) {
        if (listing == null) {
            return null;
        }
        PriceObservation observation = priceObservationRepository
                .findFirstByCompetitorListingIdOrderByCapturedAtDesc(listing.getId()).orElse(null);
        if (observation == null || !observation.isUsableForAverage()) {
            return null;
        }
        return observation.getPrice();
    }

    private String statusLabel(CompetitorListing listing) {
        if (listing == null) {
            return "Chưa xác minh";
        }
        if (listing.getMatchStatus() == MatchStatus.AUTO_CONFIRMED || listing.getMatchStatus() == MatchStatus.MANUALLY_CONFIRMED) {
            return latestValidPrice(listing) != null ? "Khớp chính xác" : "Khớp, không có giá";
        }
        if (listing.getMatchStatus() == MatchStatus.REVIEW_REQUIRED) {
            return "Nghi ngờ";
        }
        return "Không tìm thấy";
    }

    private void setPrice(org.apache.poi.ss.usermodel.Cell cell, BigDecimal price) {
        if (price != null) {
            cell.setCellValue(price.doubleValue());
        }
    }

    private String safe(String value) {
        return FormulaInjectionGuard.sanitize(value == null ? "" : value);
    }
}
