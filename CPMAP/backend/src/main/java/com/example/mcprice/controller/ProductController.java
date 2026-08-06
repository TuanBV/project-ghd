package com.example.mcprice.controller;

import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.AliasCreateRequest;
import com.example.mcprice.dto.AliasDto;
import com.example.mcprice.dto.ProductDetailDto;
import com.example.mcprice.dto.ProductSummaryDto;
import com.example.mcprice.dto.ProductUpdateRequest;
import com.example.mcprice.service.ProductAliasService;
import com.example.mcprice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductAliasService productAliasService;

    @GetMapping
    public PageResponse<ProductSummaryDto> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.of(productService.search(keyword, category, availability, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ProductDetailDto getDetail(@PathVariable Long id) {
        return productService.getDetail(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public ProductDetailDto update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    @PostMapping("/{id}/aliases")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    public AliasDto createAlias(@PathVariable Long id, @Valid @RequestBody AliasCreateRequest request) {
        return productAliasService.create(id, request);
    }
}
