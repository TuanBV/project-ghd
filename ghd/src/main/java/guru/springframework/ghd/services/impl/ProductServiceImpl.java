package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.policy.ProductSummaryDTO;
import guru.springframework.ghd.dto.product.*;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.entities.ProductImage;
import guru.springframework.ghd.entities.ProductSimilar;
import guru.springframework.ghd.mappers.ProductImageMapper;
import guru.springframework.ghd.mappers.ProductMapper;
import guru.springframework.ghd.repositories.ProductImageRepository;
import guru.springframework.ghd.repositories.ProductRepository;
import guru.springframework.ghd.repositories.ProductSimilarRepository;
import guru.springframework.ghd.services.FileStorageService;
import guru.springframework.ghd.services.ProductService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.config.CacheConfig.PRODUCTS;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_LATEST;
import static guru.springframework.ghd.config.CacheConfig.PRODUCTS_RELATED;
import static guru.springframework.ghd.utils.CommonUtil.generateSlug;
import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSimilarRepository productSimilarRepository;
    private final ProductMapper productMapper;

    private final FileStorageService fileStorageService;

    private final String SUB_FOLDER = "products";
    private final ProductImageMapper productImageMapper;

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true)
    })
    public ProductResponse addProduct(ProductRequest request,
                                      MultipartFile mainImage,
                                      List<MultipartFile> galleryFiles) {

        // Handel product
        Product product = productMapper.toEntity(request);
        if (product.getStatus() == null) product.setStatus(1);
        String rawSlugSource = String.format("%s %s %s %s",
                request.getTitle(),
                request.getSku(),
                request.getColor(),
                request.getSize()
        );
        if (mainImage != null && !mainImage.isEmpty()) {
            product.setImage(handleImageUpload(mainImage, SUB_FOLDER));
        } else if (Boolean.TRUE.equals(request.getDuplicate())
                && request.getExistingImageMain() != null
                && !request.getExistingImageMain().trim().isEmpty()) {
            product.setImage(request.getExistingImageMain().trim());
        }

        product.setSlug(generateSlug(rawSlugSource));
        Product savedProduct = productRepository.save(product);
        String savedProductId = savedProduct.getId();
        // Handle similar product
        handleProductGroupForCreate(savedProduct, request);

        // Handle save image list of product
        handleGalleryImagesForCreate(savedProductId, request, galleryFiles);

        // Save database
        savedProduct = productRepository.save(savedProduct);

        return productMapper.productToProductResponse(savedProduct);
    }

    private void handleGalleryImagesForCreate(String productId,
                                              ProductRequest request,
                                              List<MultipartFile> galleryFiles) {
        if (request.getGalleryOrder() == null || request.getGalleryOrder().isEmpty()) {
            if (galleryFiles == null || galleryFiles.isEmpty()) {
                return;
            }

            int sortOrder = 0;

            for (MultipartFile file : galleryFiles) {
                if (file == null || file.isEmpty()) continue;

                String url = fileStorageService.saveWithSubFolder(file, SUB_FOLDER);

                ProductImage newImg = new ProductImage();
                newImg.setProductId(productId);
                newImg.setImageUrl(url);
                newImg.setSortOrder(sortOrder++);

                productImageRepository.save(newImg);
            }

            return;
        }

        Map<String, MultipartFile> newFileMap = new HashMap<>();

        if (galleryFiles != null && request.getNewGalleryFileIds() != null) {
            for (int i = 0; i < galleryFiles.size(); i++) {
                if (i < request.getNewGalleryFileIds().size()) {
                    newFileMap.put(request.getNewGalleryFileIds().get(i), galleryFiles.get(i));
                }
            }
        }

        for (GalleryOrderItem item : request.getGalleryOrder()) {
            if ("old".equals(item.getType())) {
                if (item.getImageUrl() == null || item.getImageUrl().trim().isEmpty()) {
                    continue;
                }

                ProductImage oldImageAsNewRecord = new ProductImage();
                oldImageAsNewRecord.setProductId(productId);
                oldImageAsNewRecord.setImageUrl(item.getImageUrl().trim());
                oldImageAsNewRecord.setSortOrder(item.getSortOrder());

                productImageRepository.save(oldImageAsNewRecord);
            }

            if ("new".equals(item.getType())) {
                MultipartFile file = newFileMap.get(item.getFileId());

                if (file == null || file.isEmpty()) {
                    continue;
                }

                String url = fileStorageService.saveWithSubFolder(file, SUB_FOLDER);

                ProductImage newImg = new ProductImage();
                newImg.setProductId(productId);
                newImg.setImageUrl(url);
                newImg.setSortOrder(item.getSortOrder());

                productImageRepository.save(newImg);
            }
        }
    }

    private void handleProductGroupForCreate(Product savedProduct, ProductRequest request) {
        String savedProductId = savedProduct.getId();

        Set<String> newMemberIds = new LinkedHashSet<>();

        if (request.getProductGroup() != null) {
            request.getProductGroup().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(id -> !id.isEmpty())
                    .forEach(newMemberIds::add);
        }
        if (request.getDuplicateSourceId() != null && !request.getDuplicateSourceId().trim().isEmpty()) {
            newMemberIds.add(request.getDuplicateSourceId().trim());
        }
        newMemberIds.remove(savedProductId);
        newMemberIds.add(savedProductId);
        if (newMemberIds.size() <= 1) {
            savedProduct.setGroupId(null);
            return;
        }

        List<String> memberIds = new ArrayList<>(newMemberIds);
        List<String> affectedGroupIds = productRepository.findGroupIdsByIds(memberIds);

        productRepository.updateGroupIdToNullForIds(newMemberIds);

        ProductSimilar productSimilar = new ProductSimilar();
        productSimilar.setProductGroup(String.join(",", memberIds));
        String newGroupId = productSimilarRepository.save(productSimilar).getId();

        productRepository.updateGroupIdForList(newGroupId, memberIds);

        savedProduct.setGroupId(newGroupId);

        if (affectedGroupIds != null && !affectedGroupIds.isEmpty()) {
            List<String> oldGroupIds = affectedGroupIds.stream()
                    .filter(Objects::nonNull)
                    .filter(gid -> !gid.equals(newGroupId))
                    .distinct()
                    .toList();

            if (!oldGroupIds.isEmpty()) {
                syncSpecificGroups(oldGroupIds);
            }
        }
    }

    @Override
    public Page<ProductResponse> getList(String title, String categoryId, String brandId, Integer status,
                                         String sortField, String sortDir, Integer pageNumber, Integer pageSize) {

        String searchTitle = (title != null && !title.trim().isEmpty()) ? "%" + title.trim() + "%" : null;

        String catId = (categoryId != null && !categoryId.trim().isEmpty()) ? categoryId : null;
        String bId = (brandId != null && !brandId.trim().isEmpty()) ? brandId : null;

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<ProductProjection> rawData = productRepository.findAllNative(searchTitle, catId, bId, status, pageable);

        return rawData.map(item -> ProductResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .categoryName(item.getCategoryName())
                .brandName(item.getBrandName())
                .status(item.getStatus())
                .image(item.getImage())
                .priceRange(item.getPriceRange())
                .totalStock(item.getTotalStock() != null ? item.getTotalStock().intValue() : 0)
                .description(item.getDescription())
                .build());
    }

    @Override
    public ProductDetailResponse getById(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + productId));

        return mapToProductResponse(product);
    }

    private ProductDetailResponse mapToProductResponse(Product p) {
        ProductDetailResponse res = new ProductDetailResponse();

        // Set value for product
        res.setId(p.getId());
        res.setTitle(p.getTitle());
        res.setVariantName(p.getVariantName());
        res.setCategoryId(p.getCategoryId());
        res.setBrandId(p.getBrandId());
        res.setContent(p.getContent());
        res.setPolicyId(p.getPolicyId());
        res.setSpecification(p.getSpecification());
        res.setStatus(p.getStatus());
        res.setSku(p.getSku());
        res.setPrice(p.getPrice());
        res.setSalePrice(p.getSalePrice());
        res.setStockQty(p.getStockQty());
        res.setColor(p.getColor());
        res.setSize(p.getSize());
        res.setImage(p.getImage());
        res.setSoldCount(p.getSoldCount());
        res.setDescription(p.getDescription());

        // Get list image of product
        List<ProductImageResponse> images = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(p.getId())
                .stream()
                .map(productImageMapper::productImageToProductImageResponse)
                .collect(Collectors.toList());
        res.setListImages(images);

        List<ProductSummaryDTO> products = productRepository.findAllByGroupId(p.getGroupId(), p.getId());

        res.setProductGroup(products);
        return res;
    }

    @Transactional
    public void syncSpecificGroups(List<String> groupIds) {
        for (String gid : groupIds) {
            List<String> currentMemberIds = productRepository.findAllIdsByGroupId(gid);

            if (currentMemberIds.size() <= 1) {
                productRepository.updateGroupIdToNull(gid);
                productSimilarRepository.deleteById(gid);
            } else {
                productSimilarRepository.findById(gid).ifPresent(ps -> {
                    ps.setProductGroup(String.join(",", currentMemberIds));
                    productSimilarRepository.save(ps);
                });
            }
        }
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_LATEST, allEntries = true),
            @CacheEvict(cacheNames = PRODUCTS_RELATED, allEntries = true)
    })
    public ProductResponse updateProduct(String productId, ProductRequest productRequest,
                                         MultipartFile mainImage,
                                         List<MultipartFile> galleryFiles) {
        // Check product exist
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + productId));

        // Handle similar product
        if (productRequest.getProductGroup() != null) {
            Set<String> newMemberIds = new HashSet<>(productRequest.getProductGroup());
            newMemberIds.add(productId);

            List<String> affectedGroupIds = productRepository.findGroupIdsByIds(newMemberIds);

            productRepository.updateGroupIdToNullForIds(newMemberIds);

            if (!productRequest.getProductGroup().isEmpty()) {
                ProductSimilar productSimilar = new ProductSimilar();
                productSimilar.setProductGroup(String.join(",", newMemberIds));
                String newGroupId = productSimilarRepository.save(productSimilar).getId();

                productRepository.updateGroupIdForList(newGroupId, new ArrayList<>(newMemberIds));
                product.setGroupId(newGroupId);
            } else {
                product.setGroupId(null);
            }

            if (!affectedGroupIds.isEmpty()) {
                syncSpecificGroups(affectedGroupIds);
            }
        }

        // Update information of product
        product.setTitle(productRequest.getTitle());
        product.setVariantName(productRequest.getVariantName());
        product.setCategoryId(productRequest.getCategoryId());
        product.setBrandId(productRequest.getBrandId());
        product.setPolicyId(productRequest.getPolicyId());
        product.setDescription(productRequest.getDescription());
        product.setContent(productRequest.getContent());
        product.setSpecification(productRequest.getSpecification());
        product.setStatus(productRequest.getStatus());
        product.setStockQty(productRequest.getStockQty());
        product.setSku(productRequest.getSku());
        product.setPrice(BigDecimal.valueOf(productRequest.getPrice()));
        product.setSalePrice(BigDecimal.valueOf(productRequest.getSalePrice()));
        product.setColor(productRequest.getColor());
        product.setSize(productRequest.getSize());

        if (mainImage != null) {
            product.setImage(handleImageUpload(mainImage, SUB_FOLDER));
        }

        // Save product
        product = productRepository.save(product);

        if (productRequest.getRemovedImages() != null && !productRequest.getRemovedImages().isEmpty()) {
            productRequest.getRemovedImages().forEach(imgUrl -> {
                // Delete image in storage
                fileStorageService.delete(imgUrl);

                // Delete record in database
                productImageRepository.deleteAllByImageUrl(imgUrl);
            });
        }

        // Handle list image of product with sort order
        handleGalleryImagesWithSortOrder(product.getId(), productRequest, galleryFiles);

        // Response return
        return productMapper.productToProductResponse(product);
    }

    private void handleGalleryImagesWithSortOrder(String productId,
                                                  ProductRequest productRequest,
                                                  List<MultipartFile> galleryFiles) {
        if (productRequest.getGalleryOrder() == null || productRequest.getGalleryOrder().isEmpty()) {
            if (galleryFiles != null && !galleryFiles.isEmpty()) {
                int startSortOrder = productImageRepository.findByProductId(productId).size();

                for (int i = 0; i < galleryFiles.size(); i++) {
                    MultipartFile file = galleryFiles.get(i);

                    if (file == null || file.isEmpty()) continue;

                    String url = fileStorageService.saveWithSubFolder(file, SUB_FOLDER);

                    ProductImage newImg = new ProductImage();
                    newImg.setImageUrl(url);
                    newImg.setProductId(productId);
                    newImg.setSortOrder(startSortOrder + i);

                    productImageRepository.save(newImg);
                }
            }

            return;
        }

        Map<String, MultipartFile> newFileMap = new HashMap<>();

        if (galleryFiles != null && productRequest.getNewGalleryFileIds() != null) {
            for (int i = 0; i < galleryFiles.size(); i++) {
                if (i < productRequest.getNewGalleryFileIds().size()) {
                    newFileMap.put(productRequest.getNewGalleryFileIds().get(i), galleryFiles.get(i));
                }
            }
        }

        Map<String, ProductImage> oldImageMap = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId)
                .stream()
                .collect(Collectors.toMap(
                        ProductImage::getImageUrl,
                        img -> img,
                        (a, b) -> a
                ));

        for (GalleryOrderItem item : productRequest.getGalleryOrder()) {
            if ("old".equals(item.getType())) {
                ProductImage oldImg = oldImageMap.get(item.getImageUrl());

                if (oldImg != null) {
                    oldImg.setSortOrder(item.getSortOrder());
                    productImageRepository.save(oldImg);
                }
            }

            if ("new".equals(item.getType())) {
                MultipartFile file = newFileMap.get(item.getFileId());

                if (file != null && !file.isEmpty()) {
                    String url = fileStorageService.saveWithSubFolder(file, SUB_FOLDER);

                    ProductImage newImg = new ProductImage();
                    newImg.setImageUrl(url);
                    newImg.setProductId(productId);
                    newImg.setSortOrder(item.getSortOrder());

                    productImageRepository.save(newImg);
                }
            }
        }
    }

    @Override
    public Page<ProductDetailClientResponse> searchProduct(
            String title, String categoryTitle, String brandTitle, Double minPrice, Double maxPrice,
            Integer status, String sortField, String sortDir, Integer pageNumber, Integer pageSize) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

        Page<IProductDetailClient> rawData = productRepository.searchProduct(
                title, categoryTitle, brandTitle, status, minPrice, maxPrice, pageable);

        return  rawData.map(item -> {
            return toProductDetailClientResponse(item, new ArrayList<>());
        });
    }

    @Override
    @Cacheable(cacheNames = PRODUCTS, key = "'slug:' + #slug")
    public ProductDetailClientResponse getBySlug(String slug) {
        IProductDetailClient product = productRepository.findBySlug(slug);

        List<ProductImageResponse> listImage = productImageRepository.findByProductId(product.getId())
                .stream()
                .map(productImageMapper::productImageToProductImageResponse)
                .collect(Collectors.toList());

        if (listImage.isEmpty() && product.getImage() != null && !product.getImage().isEmpty()) {
            ProductImageResponse mainImage = new ProductImageResponse();
            mainImage.setImageUrl(product.getImage());
            listImage.add(mainImage);
        }

        // Convert from entity to dto
        return toProductDetailClientResponse(product, listImage);
    }

    @Override
    @Cacheable(cacheNames = PRODUCTS, key = "'idProduct:' + #idProduct")
    public ProductDetailClientResponse getByIdProduct(String idProduct) {
        IProductDetailClient product = productRepository.findByIdProduct(idProduct);

        List<ProductImageResponse> listImage = productImageRepository.findByProductId(product.getId())
                .stream()
                .map(productImageMapper::productImageToProductImageResponse)
                .collect(Collectors.toList());

        if (listImage.isEmpty() && product.getImage() != null && !product.getImage().isEmpty()) {
            ProductImageResponse mainImage = new ProductImageResponse();
            mainImage.setImageUrl(product.getImage());
            listImage.add(mainImage);
        }

        // Convert from entity to dto
        return toProductDetailClientResponse(product, listImage);
    }

    private ProductDetailClientResponse toProductDetailClientResponse(IProductDetailClient product, List<ProductImageResponse> listImage) {
        return ProductDetailClientResponse.builder()
                .id(product.getId())
                .title(product.getTitle())
                .variantName(product.getVariantName())
                .categoryName(product.getCategoryName())
                .categoryId(product.getCategoryId())
                .brandName(product.getBrandName())
                .brandId(product.getBrandId())
                .policyId(product.getPolicyId())
                .status(product.getStatus())
                .content(product.getContent())
                .specification(product.getSpecification())
                .sku(product.getSku())
                .price(product.getPrice())
                .salePrice(product.getSalePrice())
                .stockQty(product.getStockQty())
                .color(product.getColor())
                .soldCount(product.getSoldCount())
                .size(product.getSize())
                .image(product.getImage())
                .slug(product.getSlug())
                .listImages(listImage)
                .groupId(product.getGroupId())
                .description(product.getDescription())
                .build();
    }

    @Override
    public List<ProductDetailClientResponse> findTop8ByTitleContaining(String title) {
        List<IProductDetailClient> dataSearch = productRepository.findTop8Suggestions(title);

        return dataSearch.stream()
                .map(item -> toProductDetailClientResponse(item, new ArrayList<>()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDetailClientResponse> findAllForExport(String title, String categoryId, String brandId) {
        // 1. Lấy dữ liệu sản phẩm thô
        List<IProductDetailClient> rawData = productRepository.getProductExport(title, categoryId, brandId);

        if (rawData.isEmpty()) return new ArrayList<>();

        // 2. Thu thập danh sách ID sản phẩm để lấy ảnh (tránh gọi db nhiều lần trong loop)
        List<String> productIds = rawData.stream()
                .map(IProductDetailClient::getId)
                .toList();

        // 3. Lấy toàn bộ ảnh và nhóm theo productId
        // Giả sử bạn có productImageRepository
        List<ProductImage> allImages = productImageRepository.findByProductIdIn(productIds);

        Map<String, List<ProductImageResponse>> imageMap = allImages.stream()
                .collect(Collectors.groupingBy(
                        ProductImage::getProductId,
                        Collectors.mapping(img -> ProductImageResponse.builder()
                                        .id(img.getId())
                                        .imageUrl(img.getImageUrl()) // Đảm bảo field này khớp với DTO của bạn
                                        .build(),
                                Collectors.toList())
                ));

        // 4. Map data vào Response
        return rawData.stream()
                .map(item -> {
                    // Lấy list ảnh từ map dựa theo ID sản phẩm, nếu không có thì trả về list trống
                    List<ProductImageResponse> images = imageMap.getOrDefault(item.getId(), new ArrayList<>());

                    if (images.isEmpty() && item.getImage() != null && !item.getImage().isEmpty()) {
                        images.add(ProductImageResponse.builder()
                                .imageUrl(item.getImage())
                                .build());
                    }
                    return toProductDetailClientResponse(item, images);
                })
                .toList();
    }

    // NOT cached: Spring Data's PageImpl has no Jackson 3 creator, so it cannot round-trip
    // through GenericJacksonJsonRedisSerializer (confirmed via testing - fails with
    // "Cannot construct instance of PageImpl (no Creators...)"). Caching would require
    // unwrapping to a plain List + total-count DTO instead of Page; not worth the extra
    // surface for what's already a bounded, cheap query.
    @Override
    public Page<ProductDetailClientResponse> getLatestProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        Page<IProductDetailClient> productPage = productRepository.getLatestProducts(pageable);

        return  productPage.map(item -> {
            return toProductDetailClientResponse(item, new ArrayList<>());
        });
    }

    // NOT cached - see getLatestProducts() above (Page<> cannot be Redis-cached here).
    @Override
    public Page<ProductDetailClientResponse> getLatestProductsByCategory(String categoryId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        Page<IProductDetailClient> productPage = productRepository.getLatestProductsByCategory(categoryId, pageable);

        return  productPage.map(item -> {
            return toProductDetailClientResponse(item, new ArrayList<>());
        });
    }

    @Override
    public List<ProductDetailCartResponse> findByProductId(List<String> listProductId) {
        return productRepository.findByProductId(listProductId);
    }

    @Override
    public List<ProductDetailClientResponse> getProductByGroupId(String productId) {
        return productRepository.findByGroupId(productId).stream().map(productMapper::productToProductDetailClientResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(cacheNames = PRODUCTS_RELATED, key = "'all'")
    public List<ProductDetailClientResponse> getRelatedProducts() {
        List<IProductDetailClient> products = productRepository.findTop4Related();

        return products.stream()
                .map(item -> {
                    return toProductDetailClientResponse(item, new ArrayList<>());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductSearchDTO> findByNameContainingIgnoreCase(String keyword) {
        return productRepository.findByTitleContaining(keyword);
    }
}