package com.example.mcprice.domain;

/** Thu tu uu tien pipeline ghep san pham theo yeu cau nghiep vu (tu cao xuong thap). */
public enum MatchMethod {
    EXACT_NORMALIZED_SKU,
    CONFIRMED_ALIAS,
    SKU_IN_TITLE,
    SKU_IN_URL_SLUG,
    FUZZY,
    MANUAL,
    /** URL doi thu da duoc con nguoi/report tong hop san co trong file import, khong qua pipeline tu dong. */
    IMPORTED_MAPPING
}
