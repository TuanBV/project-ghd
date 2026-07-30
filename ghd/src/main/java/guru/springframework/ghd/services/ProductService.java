package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.product.*;
import guru.springframework.ghd.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface ProductService {
    List<Product> getAll();

    @Transactional
    ProductResponse addProduct(ProductRequest request, MultipartFile mainImage, List<MultipartFile> galleryFiles);

    Page<ProductResponse> getList(
            String title, String categoryId, String brandId, Integer status,
            String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    ProductDetailResponse getById(String productId);

    ProductResponse updateProduct(
            String productId, ProductRequest productRequest,
            MultipartFile imageImage,
            List<MultipartFile> galleryFiles);

    Page<ProductDetailClientResponse> searchProduct(
            String title, String categoryTitle, String brandTitle,
            Double minPrice, Double maxPrice, Integer status,
            String sortField, String sortDir, Integer pageNumber, Integer pageSize);

    ProductDetailClientResponse getBySlug(String slug);

    ProductDetailClientResponse getByIdProduct(String slug);

    Page<ProductDetailClientResponse> getLatestProducts(int i);

    Page<ProductDetailClientResponse> getLatestProductsByCategory(String categoryId, int i);

    List<ProductDetailCartResponse> findByProductId(List<String> listProductId);

    List<ProductDetailClientResponse> getProductByGroupId(String productId);

    List<ProductDetailClientResponse> getRelatedProducts();

    List<ProductSearchDTO> findByNameContainingIgnoreCase(String name);

    List<ProductDetailClientResponse> findTop8ByTitleContaining(String ten);

    List<ProductDetailClientResponse> findAllForExport(String title, String categoryId, String brandId);
}