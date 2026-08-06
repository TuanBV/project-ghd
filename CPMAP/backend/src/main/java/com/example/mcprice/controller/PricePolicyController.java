package com.example.mcprice.controller;

import com.example.mcprice.dto.PricePolicyDto;
import com.example.mcprice.dto.PricePolicyUpdateRequest;
import com.example.mcprice.service.PricePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings/price-policy")
@RequiredArgsConstructor
public class PricePolicyController {

    private final PricePolicyService pricePolicyService;

    @GetMapping
    public PricePolicyDto get() {
        return pricePolicyService.getGlobalPolicy();
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PricePolicyDto update(@Valid @RequestBody PricePolicyUpdateRequest request) {
        return pricePolicyService.updateGlobalPolicy(request);
    }
}
