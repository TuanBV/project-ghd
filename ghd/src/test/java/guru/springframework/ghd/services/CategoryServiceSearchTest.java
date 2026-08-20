package guru.springframework.ghd.services;

import guru.springframework.ghd.AbstractIntegrationTest;
import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.category.CategoryResponse;
import guru.springframework.ghd.entities.Category;
import guru.springframework.ghd.repositories.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CategoryRepository.findAll(Pageable) từng là native query nhận Pageable có Sort -
 * Spring Data JPA không chèn ORDER BY an toàn vào native SQL, ném lỗi 500 trên MỌI
 * request tới GET /api/v1/category không có "title" (đúng lớp bug với
 * OrdersRepository.search, xem OrdersServiceImplIntegrationTest). Test này dùng ĐÚNG
 * tham số mặc định của CategoryController để không tái diễn.
 */
@SpringBootTest
class CategoryServiceSearchTest extends AbstractIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void getListWithDefaultAdminSortDoesNotThrow() {
        categoryRepository.save(Category.builder()
                .title("Test Category " + UUID.randomUUID())
                .build());

        Page<CategoryResponse> page = categoryService.getList(
                null, DefaultPage.ID, DefaultPage.DESC, DefaultPage.PAGE, DefaultPage.SIZE_CLIENT);

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }
}
