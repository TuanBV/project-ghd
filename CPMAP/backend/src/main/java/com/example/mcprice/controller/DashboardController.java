package com.example.mcprice.controller;

import com.example.mcprice.dto.DashboardCompetitorHealthDto;
import com.example.mcprice.dto.DashboardPriceTrendsDto;
import com.example.mcprice.dto.DashboardSummaryDto;
import com.example.mcprice.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryDto summary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/price-trends")
    public DashboardPriceTrendsDto priceTrends() {
        return dashboardService.getPriceTrends();
    }

    @GetMapping("/competitor-health")
    public DashboardCompetitorHealthDto competitorHealth() {
        return dashboardService.getCompetitorHealth();
    }
}
