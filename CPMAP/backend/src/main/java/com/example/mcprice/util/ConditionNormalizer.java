package com.example.mcprice.util;

public final class ConditionNormalizer {

    public enum Condition {
        NEW, USED, REFURBISHED, UNKNOWN
    }

    private ConditionNormalizer() {
    }

    public static Condition normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Condition.UNKNOWN;
        }
        String collapsed = raw.trim().toLowerCase();
        if (collapsed.equals("new")) {
            return Condition.NEW;
        }
        if (collapsed.equals("used")) {
            return Condition.USED;
        }
        if (collapsed.equals("refurbished")) {
            return Condition.REFURBISHED;
        }
        return Condition.UNKNOWN;
    }
}
