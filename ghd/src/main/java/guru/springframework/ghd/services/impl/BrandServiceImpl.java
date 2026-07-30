package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.brand.BrandRequest;
import guru.springframework.ghd.dto.brand.BrandResponse;
import guru.springframework.ghd.entities.Brand;
import guru.springframework.ghd.mappers.BrandMapper;
import guru.springframework.ghd.repositories.BrandRepository;
import guru.springframework.ghd.services.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static guru.springframework.ghd.config.CacheConfig.BRANDS;
import static guru.springframework.ghd.config.CacheConfig.NEWS;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_LATEST;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_RELATED;

import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.utils.CommonUtil.toSlug;
import static guru.springframework.ghd.utils.PaginationUtil.getPageRequest;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    private final String SUB_FOLDER = "brands";

    @Override
    public Page<BrandResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, sortField, sortDir);
        Page<Brand> brandPage;

        if (StringUtils.hasText(title)) {
            brandPage = brandRepository.findAllByTitleContainingIgnoreCase(title, pageRequest);
        } else {
            brandPage = brandRepository.findAll(pageRequest);
        }

        return brandPage.map(brandMapper::brandToBrandResponse);
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize, String sortField, String sortDir) {
        return getPageRequest(pageNumber, pageSize, sortField, sortDir);
    }


    @Override
    public Optional<BrandResponse> getById(String brandId) {
        return brandRepository.findById(brandId).map(brandMapper::brandToBrandResponse);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = BRANDS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true),
            @CacheEvict(cacheNames = NEWS, allEntries = true)
    })
    public BrandResponse addBrand(BrandRequest request) {
        Brand newBrand = Brand.builder()
                .title(request.getTitle())
                .slug(toSlug(request.getTitle()))
                .build();
        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            String filePath = handleImageUpload(request.getLogo(), SUB_FOLDER);
            newBrand.setLogo(filePath);
        }

        Brand savedBrand = brandRepository.save(newBrand);
        return brandMapper.brandToBrandResponse(savedBrand);
    }

    @Override
    @Cacheable(cacheNames = BRANDS, key = "'all'")
    public List<BrandResponse> getAll() {
        return brandRepository.getAll().stream().map(brandMapper::brandToBrandResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = BRANDS, key = "'notNullLogo'")
    public List<BrandResponse> getAllNotNullLogo() {
        return brandRepository.getAllNotNullLogo().stream().map(brandMapper::brandToBrandResponse).collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = BRANDS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true),
            @CacheEvict(cacheNames = NEWS, allEntries = true)
    })
    public Optional<BrandResponse> updateById(String brandId, BrandRequest request) {
        return brandRepository.findById(brandId).map(exists -> {
            exists.setTitle(request.getTitle());
            exists.setSlug(toSlug(request.getTitle()));

            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                exists.setLogo(handleImageUpload(request.getLogo(), SUB_FOLDER));
            }

            return brandMapper.brandToBrandResponse(brandRepository.save(exists));
        });
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = BRANDS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true),
            @CacheEvict(cacheNames = NEWS, allEntries = true)
    })
    public void deleteById(String brandId) {
        brandRepository.findById(brandId).ifPresent(brand -> {
            brand.setDelFlag(1);
            brandRepository.save(brand);
        });
    }
}
