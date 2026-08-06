package com.example.mcprice.controller;

import com.example.mcprice.dto.PageResponse;
import com.example.mcprice.dto.CompetitorDto;
import com.example.mcprice.dto.CompetitorUpsertRequest;
import com.example.mcprice.service.CompetitorService;
import com.example.mcprice.service.CompetitorDiscoveryTriggerService;
import com.example.mcprice.dto.CompetitorListingDto;
import com.example.mcprice.repository.CompetitorListingRepository;
import com.example.mcprice.service.MatchingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/competitors")
@RequiredArgsConstructor
public class CompetitorController {

    private final CompetitorService competitorService;
    private final CompetitorDiscoveryTriggerService discoveryTriggerService;
    private final CompetitorListingRepository competitorListingRepository;
    private final MatchingService matchingService;

    @GetMapping
    public List<CompetitorDto> findAll() {
        return competitorService.findAll();
    }

    /**
     * Tao doi thu xong (transaction da commit) MOI kich hoat auto-discovery — goi luc con dang
     * @Transactional trong CompetitorService se gay race condition, luong @Async doc DB truoc
     * khi dong Competitor duoc commit va bao loi "Competitor khong ton tai".
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CompetitorDto create(@Valid @RequestBody CompetitorUpsertRequest request) {
        CompetitorDto created = competitorService.create(request);
        discoveryTriggerService.autoDiscoverOnCreate(created.id(), currentActor());
        return created;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CompetitorDto update(@PathVariable Long id, @Valid @RequestBody CompetitorUpsertRequest request) {
        return competitorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        competitorService.delete(id);
    }

    /** Kich hoat lai (thu cong) sitemap discovery ngam cho 1 doi thu, tra ve jobRunId de FE poll tien do. */
    @PostMapping("/{id}/discover")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'OPERATOR')")
    public Map<String, Long> discover(@PathVariable Long id) {
        Long jobRunId = discoveryTriggerService.resolveSitemapAndTrigger(id, currentActor());
        return Map.of("jobRunId", jobRunId);
    }

    /** Danh sach URL/san pham da khop duoc voi doi thu nay qua sitemap discovery hoac crawl. */
    @GetMapping("/{id}/listings")
    public PageResponse<CompetitorListingDto> listings(@PathVariable Long id,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        var result = competitorListingRepository.findByCompetitorId(id, PageRequest.of(page, size))
                .map(matchingService::toDto);
        return PageResponse.of(result);
    }

    private String currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "SYSTEM" : auth.getName();
    }
}
