package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.product.ProductDetailClientResponse;
import guru.springframework.ghd.dto.product.ProductRequest;
import guru.springframework.ghd.dto.product.ProductResponse;
import guru.springframework.ghd.entities.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product productResponseToProduct(ProductResponse productResponse);

    ProductResponse productToProductResponse(Product product);

    ProductDetailClientResponse productToProductDetailClientResponse(Product product);

    Product toEntity(ProductRequest productRequest);
}
