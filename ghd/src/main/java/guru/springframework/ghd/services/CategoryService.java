package guru.springframework.ghd.services;


import guru.springframework.ghd.dto.category.CategoryRequest;
import guru.springframework.ghd.dto.category.CategoryResponse;
import org.springframework.data.domain.Page;

import java.util.*;

public interface CategoryService {
    Page<CategoryResponse> getList(String title, String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    Optional<CategoryResponse> getById(String categoryId);

    Optional<CategoryResponse> updateById(String categoryId, CategoryRequest category);

    void deleteById(String categoryId);

    CategoryResponse addCategory(CategoryRequest category);

    List<CategoryResponse> getAll();

    List<CategoryResponse> getAllNotNullLogo();
}
