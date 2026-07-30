package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.category.CategoryRequest;
import guru.springframework.ghd.dto.category.CategoryResponse;
import guru.springframework.ghd.entities.Category;
import guru.springframework.ghd.mappers.CategoryMapper;
import guru.springframework.ghd.repositories.CategoryRepository;
import guru.springframework.ghd.services.CategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static guru.springframework.ghd.config.CacheConfig.CATEGORIES;
import static guru.springframework.ghd.config.CacheConfig.NEWS;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_LATEST;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_RELATED;

import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.utils.CommonUtil.generateSlug;
import static guru.springframework.ghd.utils.PaginationUtil.getPageRequest;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    private final String SUB_FOLDER = "categories";

    @Override
    public Page<CategoryResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {
        PageRequest pageRequest = buildPageRequest(pageNumber, pageSize, sortField, sortDir);

        Page<Category> categoryPage;

        if (StringUtils.hasText(title)) {
            categoryPage = categoryRepository.findAllByTitleContainingIgnoreCase(title, pageRequest);
        } else {
            categoryPage = categoryRepository.findAll(pageRequest);
        }

        return categoryPage.map(categoryMapper::categoryToCategoryResponse);
    }

    private PageRequest buildPageRequest(Integer pageNumber, Integer pageSize, String sortField, String sortDir) {
        return getPageRequest(pageNumber, pageSize, sortField, sortDir);
    }

    @Override
    public Optional<CategoryResponse> getById(String categoryId) {
        return categoryRepository.findById(categoryId).map(categoryMapper::categoryToCategoryResponse);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true),
            @CacheEvict(cacheNames = NEWS, allEntries = true)
    })
    public CategoryResponse addCategory(CategoryRequest request) {
        Integer priority = request.getPriority();

        if (priority == null || priority <= 0) {
            priority = categoryRepository.findMaxPriority() + 1;
        } else {
            categoryRepository.shiftPrioritiesForInsert(priority);
        }

        Category newCategory = Category.builder()
                .title(request.getTitle())
                .slug(generateSlug(request.getTitle()))
                .priority(priority)
                .build();

        if (request.getLogo() != null && !request.getLogo().isEmpty()) {
            String filePath = handleImageUpload(request.getLogo(), SUB_FOLDER);
            newCategory.setLogo(filePath);
        }

        Category savedCategory = categoryRepository.save(newCategory);
        return categoryMapper.categoryToCategoryResponse(savedCategory);
    }

    @Override
    @Cacheable(cacheNames = CATEGORIES, key = "'all'")
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAllByOrderByPriorityAsc().stream().map(categoryMapper::categoryToCategoryResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = CATEGORIES, key = "'notNullLogo'")
    public List<CategoryResponse> getAllNotNullLogo() {
        return categoryRepository.findAllNotNullLogo().stream().map(categoryMapper::categoryToCategoryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true),
            @CacheEvict(cacheNames = NEWS, allEntries = true)
    })
    public Optional<CategoryResponse> updateById(String categoryId, CategoryRequest request) {
        return categoryRepository.findById(categoryId).map(exists -> {
            Integer newPriority = request.getPriority();
            Integer oldPriority = exists.getPriority();

            if (newPriority != null && newPriority > 0 && !newPriority.equals(oldPriority)) {
                if (oldPriority == null) {
                    categoryRepository.shiftPrioritiesForInsert(newPriority);
                } else if (newPriority < oldPriority) {
                    categoryRepository.shiftDownWhenMoveUp(categoryId, oldPriority, newPriority);
                } else {
                    categoryRepository.shiftUpWhenMoveDown(categoryId, oldPriority, newPriority);
                }

                exists.setPriority(newPriority);
            }

            exists.setTitle(request.getTitle());
            exists.setSlug(generateSlug(request.getTitle()));

            if (request.getLogo() != null && !request.getLogo().isEmpty()) {
                exists.setLogo(handleImageUpload(request.getLogo(), SUB_FOLDER));
            }

            Category savedCategory = categoryRepository.save(exists);
            return categoryMapper.categoryToCategoryResponse(savedCategory);
        });
    }

    @Override
    @Caching(evict = {
            @CacheEvict(cacheNames = CATEGORIES, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true),
            @CacheEvict(cacheNames = NEWS, allEntries = true)
    })
    public void deleteById(String categoryId) {
        categoryRepository.findById(categoryId).ifPresent(category -> {
            category.setDelFlag(1); // Soft delete
            categoryRepository.save(category);
        });
    }
}