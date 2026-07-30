package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.config.CacheConfig;
import guru.springframework.ghd.dto.category.CategoryRequest;
import guru.springframework.ghd.entities.Category;
import guru.springframework.ghd.repositories.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the real Redis-backed cache (via {@link AbstractIntegrationTest}'s
 * Testcontainers Redis) with the JPA repository mocked out, so we can assert on
 * repository invocation counts: cache hits must not reach the repository, and a
 * write must evict so the next read goes back to the repository.
 */
@SpringBootTest
class CategoryServiceCacheTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @BeforeEach
    void clearCache() {
        Objects.requireNonNull(cacheManager.getCache(CacheConfig.CATEGORIES)).clear();
    }

    @Test
    void repeatedReadsHitCacheInsteadOfRepository() {
        when(categoryRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of());

        categoryService.getAll();
        categoryService.getAll();
        categoryService.getAll();

        verify(categoryRepository, times(1)).findAllByOrderByPriorityAsc();
    }

    @Test
    void differentReadMethodsAreCachedUnderDistinctKeys() {
        when(categoryRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of());
        when(categoryRepository.findAllNotNullLogo()).thenReturn(List.of());

        categoryService.getAll();
        categoryService.getAllNotNullLogo();
        categoryService.getAll();
        categoryService.getAllNotNullLogo();

        verify(categoryRepository, times(1)).findAllByOrderByPriorityAsc();
        verify(categoryRepository, times(1)).findAllNotNullLogo();
    }

    @Test
    void writingANewCategoryEvictsTheCache() {
        when(categoryRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of());
        when(categoryRepository.findMaxPriority()).thenReturn(0);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.getAll();
        categoryService.getAll();
        verify(categoryRepository, times(1)).findAllByOrderByPriorityAsc();

        CategoryRequest request = new CategoryRequest();
        request.setTitle("Cache eviction test category");
        categoryService.addCategory(request);

        categoryService.getAll();
        verify(categoryRepository, times(2)).findAllByOrderByPriorityAsc();
    }
}
