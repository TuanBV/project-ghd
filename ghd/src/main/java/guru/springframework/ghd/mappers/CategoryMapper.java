package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.category.CategoryResponse;
import guru.springframework.ghd.entities.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category categoryResponseToCategory(CategoryResponse categoryResponse);

    CategoryResponse categoryToCategoryResponse(Category category);
}
