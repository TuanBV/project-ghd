package com.example.mcprice.dto;

/** Du lieu ung vien can ghep (tu dong crawl, tu import hoac nhap tay mot URL doi thu). */
public record MatchCandidate(
        String skuRaw,
        String titleRaw,
        String url
) {
}
