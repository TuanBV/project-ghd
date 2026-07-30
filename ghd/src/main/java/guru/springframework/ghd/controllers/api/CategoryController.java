package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.category.CategoryRequest;
import guru.springframework.ghd.dto.category.CategoryResponse;
import guru.springframework.ghd.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/category")
public class CategoryController extends BaseController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<?> getCategories(
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = DefaultPage.PAGE_STRING) Integer pageNumber,
            @RequestParam(required = false, defaultValue = DefaultPage.SIZE_CLIENT_STRING) Integer pageSize,
            @RequestParam(required = false, defaultValue = DefaultPage.ID) String sortField,
            @RequestParam(required = false, defaultValue = DefaultPage.DESC) String sortDir
    ) {
        Page<CategoryResponse> categoryPage = categoryService.getList(title, sortField, sortDir, pageNumber, pageSize);
        return ok(categoryPage);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addCategory(@Valid @ModelAttribute CategoryRequest categoryRequest) {
        CategoryResponse savedCategory = categoryService.addCategory(categoryRequest);
        return ok(savedCategory);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getById(@PathVariable("categoryId") String categoryId) {
        return categoryService.getById(categoryId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{categoryId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateById(@PathVariable("categoryId") String categoryId,
                                     @Valid @ModelAttribute CategoryRequest categoryRequest) {
        Optional<CategoryResponse> updatedBrand = categoryService.updateById(categoryId, categoryRequest);

        if (updatedBrand.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ok(updatedBrand.get());
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteById(@PathVariable("categoryId") String categoryId) {
        categoryService.deleteById(categoryId);
        return ok("Deleted successfully");
    }
}
