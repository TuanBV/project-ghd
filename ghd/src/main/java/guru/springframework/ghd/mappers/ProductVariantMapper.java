package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.product.ProductRequest;
import guru.springframework.ghd.dto.product.ProductRequest;
import guru.springframework.ghd.dto.product.ProductResponse;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.entities.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {
    Product productResponseToProduct(ProductResponse productResponse);

    ProductResponse productToProductResponse(Product product);

    Product toEntity(ProductRequest productRequest);
}
