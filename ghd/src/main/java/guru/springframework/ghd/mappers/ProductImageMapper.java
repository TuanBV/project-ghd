package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.product.ProductImageResponse;
import guru.springframework.ghd.entities.ProductImage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {
    ProductImage productImageResponseToProductImage(ProductImageResponse productImageResponse);

    ProductImageResponse productImageToProductImageResponse(ProductImage productImage);
}
