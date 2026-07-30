package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.policy.PolicyCustomResponse;
import guru.springframework.ghd.dto.policy.PolicyRequest;
import guru.springframework.ghd.dto.policy.PolicyResponse;
import guru.springframework.ghd.dto.policy.ProductSummaryDTO;
import guru.springframework.ghd.entities.Policy;
import guru.springframework.ghd.mappers.PolicyMapper;
import guru.springframework.ghd.repositories.PolicyRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.services.PolicyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.config.CacheConfig.POLICIES;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_LATEST;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_RELATED;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService {

    private final PolicyRepository policyRepository;

    private final ProductRepository productRepository;

    private final PolicyMapper policyMapper;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = POLICIES, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true)
    })
    public void addPolicy(PolicyRequest request) {
        Policy policy = policyMapper.toEntity(request);
        if (policy.getIsActive() == null) {
            policy.setIsActive(1);
        }

        Policy savedPolicy = policyRepository.save(policy);

        List<String> productIds = request.getAppliedProductIds();

        if (productIds != null && !productIds.isEmpty()) {
            productRepository.updatePolicyIdByProductIds(savedPolicy.getId().toString(), productIds);
        } else {
            productRepository.updatePolicyIdForNullProducts(savedPolicy.getId().toString());
        }
    }

    @Override
    public List<PolicyResponse> getList(String name, String field, String sort) {
        List<Policy> policies = policyRepository.findListCustom(name, field, sort);

        return toPolicyResponses(policies);
    }

    @Override
    @Cacheable(cacheNames = POLICIES, key = "'all'")
    public List<PolicyResponse> getAll() {

        List<Policy> policies = policyRepository.findAll();

        return toPolicyResponses(policies);
    }

    private List<PolicyResponse> toPolicyResponses(List<Policy> policies) {
        return policies.stream()
                .map(policy -> {
                    PolicyResponse res = new PolicyResponse();
                    res.setId(policy.getId().toString());
                    res.setPackageName(policy.getPackageName());
                    res.setPolicies(policy.getPolicies());
                    res.setGifts(policy.getGifts());
                    res.setAfterSales(policy.getAfterSales());
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = POLICIES, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true)
    })
    public void updatePolicy(String id, PolicyRequest request) {
        UUID uuid = UUID.fromString(id);
        Policy entity = policyRepository.findById(uuid)
                .filter(p -> p.getDelFlag() == 0)
                .orElseThrow(() -> new RuntimeException("Gói chính sách không tồn tại hoặc đã bị xóa"));

        entity.setPackageName(request.getPackageName());
        entity.setPolicies(request.getPolicies());
        entity.setAfterSales(request.getAfterSales());
        entity.setGifts(request.getGifts());
        entity.setIsActive(request.getIsActive());
        policyRepository.save(entity);

        productRepository.clearPolicyFromProduct(id);

        List<String> newProductIds = request.getAppliedProductIds();
        if (newProductIds != null && !newProductIds.isEmpty()) {
            productRepository.updatePolicyIdByProductIds(id, newProductIds);
        } else {
            productRepository.updatePolicyIdForNullProducts(entity.getId().toString());
        }
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = POLICIES, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true)
    })
    public void deletePolicy(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Định dạng ID không hợp lệ");
        }
        Policy entity = policyRepository.findById(uuid)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gói chính sách với ID: " + id));
        if (entity.getDelFlag() == 1) {
            throw new RuntimeException("Gói chính sách này đã được xóa trước đó");
        }
        entity.setDelFlag(1);
        policyRepository.save(entity);
    }

    @Override
    public PolicyCustomResponse getById(String id) {
        UUID uuid = UUID.fromString(id);

        Policy entity = policyRepository.findById(uuid)
                .filter(p -> p.getDelFlag() == 0)

                .orElseThrow(() -> new RuntimeException("Gói chính sách không tồn tại hoặc đã bị xóa"));

        List<ProductSummaryDTO> products = productRepository.findAllByPolicyId(entity.getId().toString());
        return PolicyCustomResponse.builder()
                .id(entity.getId().toString())
                .packageName(entity.getPackageName())
                .policies(entity.getPolicies())
                .afterSales(entity.getAfterSales())
                .gifts(entity.getGifts())
                .appliedProductIds(products)
                .isActive(entity.getIsActive())
                .build();
    }

    @Override
    @Cacheable(cacheNames = POLICIES, key = "'id:' + #id")
    public PolicyResponse getPolicyById(String id) {
        Policy entity = policyRepository.findById(UUID.fromString(id))
                .filter(p -> p.getDelFlag() == 0)
                .orElseThrow(() -> new RuntimeException("Gói chính sách không tồn tại"));

        return policyMapper.policyToPolicyResponse(entity);
    }
}