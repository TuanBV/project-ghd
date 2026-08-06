package com.example.mcprice.service;

import com.example.mcprice.util.FormulaInjectionGuard;
import com.example.mcprice.domain.Product;
import com.example.mcprice.repository.ProductRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fallback: xuat lai feed dung schema MC.xlsx (17 cot) tu du lieu Product hien tai, dung khi
 * chua goi duoc Google Merchant API — nguoi dung co the upload thu cong qua Merchant Center.
 */
@Service
@RequiredArgsConstructor
public class MerchantFeedExportService {

    private static final List<String> HEADERS = List.of(
            "id", "item_group_id", "tiêu đề", "mô tả", "liên kết", "tình trạng", "giá", "còn hàng",
            "liên kết hình ảnh", "gtin", "mpn", "nhãn hiệu", "danh mục sản phẩm của Google", "loại sản phẩm",
            "nhãn tùy chỉnh 0", "nhãn tùy chỉnh 1", "nhãn tùy chỉnh 2");

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public byte[] exportXlsx() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Trang tính1");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                headerRow.createCell(i).setCellValue(HEADERS.get(i));
            }
            int rowIndex = 1;
            for (Product product : productRepository.findAll()) {
                Row row = sheet.createRow(rowIndex++);
                writeRow(row, product);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void writeRow(Row row, Product product) {
        row.createCell(0).setCellValue(safe(product.getMcOfferId()));
        row.createCell(1).setCellValue(safe(product.getItemGroupId()));
        row.createCell(2).setCellValue(safe(product.getTitle()));
        row.createCell(3).setCellValue(safe(product.getDescription()));
        row.createCell(4).setCellValue(safe(product.getProductUrl()));
        row.createCell(5).setCellValue(safe(product.getCondition()));
        row.createCell(6).setCellValue(product.getCurrentMcPrice() == null ? ""
                : product.getCurrentMcPrice().toPlainString() + " " + product.getCurrency());
        row.createCell(7).setCellValue(safe(product.getAvailability()));
        row.createCell(8).setCellValue(safe(product.getImageUrl()));
        row.createCell(9).setCellValue("");
        row.createCell(10).setCellValue("");
        row.createCell(11).setCellValue(safe(product.getBrand()));
        row.createCell(12).setCellValue(safe(product.getGoogleCategory()));
        row.createCell(13).setCellValue(safe(product.getProductType()));
        row.createCell(14).setCellValue("");
        row.createCell(15).setCellValue("");
        row.createCell(16).setCellValue("");
    }

    private String safe(String value) {
        return FormulaInjectionGuard.sanitize(value == null ? "" : value);
    }
}
