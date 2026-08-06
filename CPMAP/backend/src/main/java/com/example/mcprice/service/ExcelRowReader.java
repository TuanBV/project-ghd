package com.example.mcprice.service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Doc sheet Excel thanh danh sach header + Map moi dong. Luon lay gia tri (formula khong
 * duoc coi la nguon chan ly) — voi cell dang cong thuc, lay cached value bang DataFormatter/getNumericCellValue.
 */
final class ExcelRowReader {

    private final DataFormatter formatter = new DataFormatter();

    /**
     * File Excel nguon tron lan ca 2 dang chuan hoa Unicode cho tieng Viet co dau: mot so header
     * (vd "tiêu đề", "mô tả") luu ky tu dau duoi dang to hop (base + combining mark) trong khi
     * cac header khac (vd "liên kết") luu dang precomposed. Neu khong chuan hoa ve cung 1 dang,
     * raw.get("tiêu đề") trong code (luon la NFC) se khong khop key doc tu file, khien gan nhu
     * toan bo tieu de/mo ta san pham bi rong.
     */
    private static String normalizeKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC);
    }

    List<String> readHeaders(Sheet sheet, int headerRowIndex, int columnCount) {
        Row headerRow = sheet.getRow(headerRowIndex);
        List<String> headers = new java.util.ArrayList<>();
        for (int i = 0; i < columnCount; i++) {
            Cell cell = headerRow.getCell(i);
            String header = cell == null ? "col" + i : formatter.formatCellValue(cell).trim();
            headers.add(normalizeKey(header));
        }
        return headers;
    }

    Map<String, Object> readRow(Sheet sheet, int rowIndex, List<String> headers) {
        Row row = sheet.getRow(rowIndex);
        Map<String, Object> values = new LinkedHashMap<>();
        if (row == null) {
            return values;
        }
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i);
            values.put(headers.get(i), readCellValue(cell));
        }
        return values;
    }

    boolean isRowBlank(Map<String, Object> row) {
        return row.values().stream().allMatch(v -> v == null || v.toString().isBlank());
    }

    private Object readCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case NUMERIC -> cell.getNumericCellValue();
            case BOOLEAN -> cell.getBooleanCellValue();
            case BLANK -> null;
            case STRING -> cell.getStringCellValue().trim();
            default -> formatter.formatCellValue(cell).trim();
        };
    }
}
