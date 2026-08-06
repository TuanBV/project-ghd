package com.example.mcprice.util;

/** Chong formula injection khi export CSV/XLSX: cac gia tri bat dau bang =, +, -, @, tab, CR se bi vo hieu hoa. */
public final class FormulaInjectionGuard {

    private FormulaInjectionGuard() {
    }

    public static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}
