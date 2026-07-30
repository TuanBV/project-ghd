package guru.springframework.ghd.controllers.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.banner.BannerResponse;
import guru.springframework.ghd.dto.brand.BrandResponse;
import guru.springframework.ghd.dto.category.CategoryResponse;
import guru.springframework.ghd.dto.news.NewsResponse;
import guru.springframework.ghd.dto.policy.PolicyResponse;
import guru.springframework.ghd.dto.product.ProductDetailClientResponse;
import guru.springframework.ghd.dto.review.ReviewResponse;
import guru.springframework.ghd.dto.slider.SliderResponse;
import guru.springframework.ghd.services.*;
import guru.springframework.ghd.utils.CommonUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static guru.springframework.ghd.utils.CommonUtil.toSlug;

@Controller
@RequiredArgsConstructor
@RequestMapping("/")
public class ViewClientController {
    private final NewsService newsService;
    private final SliderService sliderService;
    private final BannerService bannerService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ProductService productService;
    private final PolicyService policyService;
    private final ReviewService reviewService;

    @GetMapping("")
    public String home(Model model) {
        // Get banner
        List<BannerResponse> banners = bannerService.getAll();
        List<BannerResponse> homeBanners = banners.stream()
                .filter(b -> "HOME_TOP".equalsIgnoreCase(b.getPosition()))
                .collect(Collectors.toList());

        model.addAttribute("banners", homeBanners);
        // Get slide
        List<SliderResponse> sliderPage = sliderService.getAll();
        List<String> imageUrls = sliderPage.stream()
                .map(SliderResponse::getImageUrl)
                .collect(Collectors.toList());
        model.addAttribute("sliderImages", imageUrls);

        // Get category
        List<CategoryResponse> categories = categoryService.getAllNotNullLogo();
        model.addAttribute("categories", categories);

        // Giả sử bạn có phương thức getLatestProducts trong ProductService
        Page<ProductDetailClientResponse> latestProducts = productService.getLatestProducts(10);
        model.addAttribute("products", latestProducts.getContent());

        String tiviId = "5b0fe4e4-0cac-4320-984d-fc192431fc59";
        String robothutmuiId = "e003e476-db57-4293-bef7-cfdbbc30b6a1";
        Page<ProductDetailClientResponse> tiviProducts = productService.getLatestProductsByCategory(tiviId, 4);
        //Page<ProductDetailClientResponse> maylockkProducts = productService.getLatestProductsByCategory("a27632a0-6aa1-4431-861d-e32ce21c348f", 4);
        Page<ProductDetailClientResponse> robothutmuiProducts = productService.getLatestProductsByCategory(robothutmuiId, 4);

        model.addAttribute("listSpecial", List.of(
                Map.of(
                        "title", "Tivi thông minh",
                        "slug", "tivi",
                        "link", "Tivi",
                        "listProduct", tiviProducts.getContent(),
                        "banner", "/client/images/tivi_xiaomi_2026.webp"
                ),
                Map.of(
                        "title", "Robot hút bụi",
                        "slug", "robot-hut-bui",
                        "link", "Robot hút bụi",
                        "listProduct", robothutmuiProducts.getContent(),
                        "banner", "/client/images/anh_robot_lau_nha.webp"
                )
        ));

        List<BrandResponse> brands = brandService.getAllNotNullLogo();
        model.addAttribute("brands", brands);

        return "client/home";
    }

    @GetMapping("ve-chung-toi")
    public String about(Model model) {
        return "client/about";
    }

    @GetMapping("chinh-sach/doi-tra")
    public String returnExchange(Model model) {
        return "client/policy/return-exchange";
    }

    @GetMapping("chinh-sach/bao-hanh")
    public String warranty(Model model) {
        return "client/policy/warranty";
    }

    @GetMapping("chinh-sach/thanh-toan")
    public String payment(Model model) {
        return "client/policy/payment";
    }

    @GetMapping("chinh-sach/dieu-khoan-su-dung-va-bao-mat")
    public String termsOfUseAndPrivacy(Model model) {
        return "client/policy/terms-of-use-and-privacy";
    }

    @GetMapping("chinh-sach/quy-dinh-chung")
    public String generalRegulations(Model model) {
        return "client/policy/general-regulations";
    }

    @GetMapping("chinh-sach/van-chuyen")
    public String shipping(Model model) {
        return "client/policy/shipping";
    }

    @GetMapping("lien-he")
    public String contact(Model model) {
        return "client/contact";
    }

    @GetMapping("tin-tuc")
    public String news(
            @RequestParam(name = "tu-khoa", required = false) String title,
            @RequestParam(name = "trang", defaultValue = DefaultPage.PAGE_STRING) int page,
            @RequestParam(name = "so-ban-ghi", defaultValue = "5") int size,
            @RequestParam(name = "thuoc-tinh", defaultValue = "created_date") String sortField,
            @RequestParam(name = "kieu-sap-xep", defaultValue = DefaultPage.SIZE_CLIENT_STRING) String sortDir,
            Model model) {

        Page<NewsResponse> newsPage = newsService.getList(title, sortField, sortDir, page, size);
        List<NewsResponse> newsFeatured = newsService.getListFeatured();

        if (newsFeatured == null || newsFeatured.isEmpty()) {
            List<NewsResponse> allNews = newsPage.getContent();
            newsFeatured = allNews.stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        model.addAttribute("titlePage", "Tin tức & Sự kiện");
        model.addAttribute("newsData", newsPage.getContent());
        model.addAttribute("newsFeatured", newsFeatured);
        model.addAttribute("totalPages", newsPage.getTotalPages());
        model.addAttribute("currentPage", page + 1);

        return "client/news/list";
    }

    @GetMapping("tin-tuc/{slug}")
    public String newsDetail(@PathVariable("slug") String slug, Model model, HttpServletRequest request) {
        NewsResponse news = newsService.getBySlug(slug);

        // Update view count of record
        newsService.updateViewCount(news.getId());
        news.setViewCount(news.getViewCount() + 1);

        Page<NewsResponse> latestNews = newsService.getList(null, DefaultPage.CREATED_DATE, DefaultPage.DESC, 0, 4);

        model.addAttribute("news", news);
        model.addAttribute("titlePage", news.getTitle());
        model.addAttribute("hotNews", latestNews.getContent());
        model.addAttribute("relatedNews", latestNews.getContent());
        return "client/news/detail";
    }

    @GetMapping("/san-pham")
    public String product(
            @RequestParam(name = "ten", required = false) String title,
            @RequestParam(name = "danh-muc", required = false) String categoryTitle,
            @RequestParam(name = "thuong-hieu", required = false) String brandTitle,
            @RequestParam(name = "gia-min", required = false) Double minPrice,
            @RequestParam(name = "gia-max", required = false) Double maxPrice,
            @RequestParam(name = "trang-thai", required = false, defaultValue = "1") Integer status,
            @RequestParam(name = "trang", defaultValue = DefaultPage.PAGE_STRING) int page,
            @RequestParam(name = "so-ban-ghi", defaultValue = DefaultPage.SIZE_STRING) int size,
            @RequestParam(name = "thuoc-tinh", defaultValue = DefaultPage.CREATED_DATE) String sortField,
            @RequestParam(name = "kieu-sap-xep", defaultValue = DefaultPage.DESC) String sortDir,
            Model model) {

        // Chú ý: searchProduct giờ trả về Page<ProductDetailClientResponse>
        Page<ProductDetailClientResponse> productPage = productService.searchProduct(
                title, categoryTitle, brandTitle, minPrice, maxPrice, status, sortField, sortDir, page, size
        );
        BannerResponse banner = bannerService.getBannerByPosition("SAN_PHAM");

        NewsResponse relatedNews = null;
        boolean hasCategory = categoryTitle != null && !categoryTitle.trim().isEmpty();
        boolean hasBrand = brandTitle != null && !brandTitle.trim().isEmpty();

        if (hasCategory || hasBrand) {
            relatedNews = newsService.getRelatedNews(categoryTitle, brandTitle);
        }

        // Lấy danh sách cho bộ lọc Sidebar
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("brands", brandService.getAll());
        model.addAttribute("banner", banner);

        // Đẩy dữ liệu ra View
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalElements", productPage.getTotalElements());

        // Giữ lại các giá trị lọc để hiển thị lại trên Form (Search giữ chỗ)
        model.addAttribute("title", title);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("relatedNews", relatedNews);

        // Param
        model.addAttribute("categoryTitle", toSlug(categoryTitle));
        model.addAttribute("brandTitle", toSlug(brandTitle));

        return "client/product/list";
    }

    @GetMapping("product/{idProduct}")
    public RedirectView productDetailById(@PathVariable("idProduct") String idProduct) {
        ProductDetailClientResponse product = productService.getByIdProduct(idProduct);

        RedirectView redirectView = new RedirectView("/san-pham/" + product.getSlug());
        redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY); // 301

        return redirectView;
    }

    @GetMapping("san-pham/{slug}")
    public String productDetail(@PathVariable("slug") String slug, Model model) throws JsonProcessingException {
        // Get product variant by slug
        ProductDetailClientResponse product = productService.getBySlug(slug);
        // Get variants have product id
        List<ProductDetailClientResponse> variants = productService.getProductByGroupId(product.getGroupId());
        //  Get related product
        List<ProductDetailClientResponse> relatedProducts = productService.getRelatedProducts();
        // Get policy
        PolicyResponse policy = null;
        if (StringUtils.hasText(product.getPolicyId())) {
            policy = policyService.getPolicyById(product.getPolicyId());
        }

        // Get list review
        List<ReviewResponse> reviews = reviewService.getByProductId(product.getId());

        //String clean = CommonUtil.getCleanDescription(product.getDescription());
        String description = CommonUtil.getCleanDescription(product.getDescription());
        model.addAttribute("product", product);
        model.addAttribute("seoDescription", description);
        model.addAttribute("variants", variants);
        model.addAttribute("policy", policy);
        model.addAttribute("relatedProducts", relatedProducts);
        model.addAttribute("titlePage", product.getTitle());
        model.addAttribute("reviews", reviews);

        ObjectMapper mapper = new ObjectMapper();

        String baseUrl = "https://greenhomeshop.vn";

        String productUrl = baseUrl + "/san-pham/" + product.getSlug();
        String productId = productUrl + "#product";
        String webpageId = productUrl + "#webpage";

        // ================= PLACE =================
        Map<String, Object> place = new LinkedHashMap<>();

        place.put("@type", "Place");
        place.put("@id", baseUrl + "/#place");

        place.put("geo", Map.of(
                "@type", "GeoCoordinates",
                "latitude", "20.941234",
                "longitude", "105.845678"
        ));

        place.put("address", Map.of(
                "@type", "PostalAddress",
                "streetAddress", "LK7-142, Khu tái định cư, Xã Tứ Hiệp, Huyện Thanh Trì",
                "addressLocality", "Hà Nội",
                "addressCountry", "VN"
        ));

        // ================= ORGANIZATION =================
        Map<String, Object> organization = new LinkedHashMap<>();

        organization.put("@type", List.of(
                "Organization",
                "Store",
                "ElectronicsStore"
        ));

        organization.put("@id", baseUrl + "/#organization");

        organization.put("name", "Green Home Shop");

        organization.put("url", baseUrl);

        organization.put("logo", Map.of(
                "@type", "ImageObject",
                "url", baseUrl + "/client/images/logo.png"
        ));

        organization.put("telephone", "+84852262666");

        organization.put("sameAs", List.of(
                "https://www.facebook.com/greenhomeshop.vn"
        ));

        organization.put("location", Map.of(
                "@id", baseUrl + "/#place"
        ));

        // ================= WEBSITE =================
        Map<String, Object> website = new LinkedHashMap<>();

        website.put("@type", "WebSite");
        website.put("@id", baseUrl + "/#website");

        website.put("url", baseUrl);

        website.put("name", "Green Home Shop");

        website.put("publisher", Map.of(
                "@id", baseUrl + "/#organization"
        ));

        // ================= BREADCRUMB =================
        Map<String, Object> breadcrumb = new LinkedHashMap<>();

        breadcrumb.put("@type", "BreadcrumbList");
        breadcrumb.put("@id", productUrl + "#breadcrumb");

        List<Map<String, Object>> breadcrumbItems = new ArrayList<>();

        breadcrumbItems.add(Map.of(
                "@type", "ListItem",
                "position", 1,
                "name", "Trang chủ",
                "item", baseUrl
        ));

        breadcrumbItems.add(Map.of(
                "@type", "ListItem",
                "position", 2,
                "name", "Sản phẩm",
                "item", baseUrl + "/san-pham"
        ));

        breadcrumbItems.add(Map.of(
                "@type", "ListItem",
                "position", 3,
                "name", product.getTitle(),
                "item", productUrl
        ));

        breadcrumb.put("itemListElement", breadcrumbItems);

        // ================= WEBPAGE =================
        Map<String, Object> webpage = new LinkedHashMap<>();

        webpage.put("@type", "WebPage");
        webpage.put("@id", webpageId);

        webpage.put("url", productUrl);

        webpage.put("name", product.getTitle());

        webpage.put("isPartOf", Map.of(
                "@id", baseUrl + "/#website"
        ));

        webpage.put("breadcrumb", Map.of(
                "@id", productUrl + "#breadcrumb"
        ));

        // ================= PRODUCT =================
        Map<String, Object> productJson = new LinkedHashMap<>();

        productJson.put("@type", "Product");

        productJson.put("@id", productId);

        productJson.put("url", productUrl);

        productJson.put("name", product.getTitle());

        productJson.put("description", description);

        productJson.put("sku", product.getId().toString());

        productJson.put("mainEntityOfPage", Map.of(
                "@id", webpageId
        ));

        // BRAND
        productJson.put("brand", Map.of(
                "@type", "Brand",
                "name",
                product.getBrandName() != null
                        ? product.getBrandName()
                        : "Green Home Shop"
        ));

        // IMAGE
        List<String> images = product.getListImages()
                .stream()
                .map(img -> baseUrl + img.getImageUrl() + "_pc.webp")
                .distinct()
                .toList();

        productJson.put("image", images);

        // ================= PRICE =================
        String price =
                product.getSalePrice() != null
                        && product.getSalePrice().compareTo(BigDecimal.ZERO) > 0
                        ? product.getSalePrice().toPlainString()
                        : product.getPrice().toPlainString();

        // ================= SHIPPING =================
        Map<String, Object> shippingDetails = new LinkedHashMap<>();

        shippingDetails.put("@type", "OfferShippingDetails");

        shippingDetails.put("shippingDestination", Map.of(
                "@type", "DefinedRegion",
                "addressCountry", "VN"
        ));

        shippingDetails.put("shippingRate", Map.of(
                "@type", "MonetaryAmount",
                "value", "0",
                "currency", "VND"
        ));

        shippingDetails.put("deliveryTime", Map.of(
                "@type", "ShippingDeliveryTime",
                "handlingTime", Map.of(
                        "@type", "QuantitativeValue",
                        "minValue", 0,
                        "maxValue", 1,
                        "unitCode", "DAY"
                ),
                "transitTime", Map.of(
                        "@type", "QuantitativeValue",
                        "minValue", 1,
                        "maxValue", 5,
                        "unitCode", "DAY"
                )
        ));

        // ================= RETURN POLICY =================
        Map<String, Object> returnPolicy = new LinkedHashMap<>();

        returnPolicy.put("@type", "MerchantReturnPolicy");

        returnPolicy.put("applicableCountry", "VN");

        returnPolicy.put(
                "returnPolicyCategory",
                "https://schema.org/MerchantReturnFiniteReturnWindow"
        );

        returnPolicy.put("merchantReturnDays", 7);

        returnPolicy.put(
                "returnMethod",
                "https://schema.org/ReturnByMail"
        );

        returnPolicy.put(
                "returnFees",
                "https://schema.org/FreeReturn"
        );

        // ================= OFFER =================
        Map<String, Object> offer = new LinkedHashMap<>();

        offer.put("@type", "Offer");

        offer.put("url", productUrl);

        offer.put("price", price);

        offer.put("priceCurrency", "VND");

        offer.put(
                "availability",
                product.getStockQty() > 0
                        ? "https://schema.org/InStock"
                        : "https://schema.org/OutOfStock"
        );

        offer.put(
                "itemCondition",
                "https://schema.org/NewCondition"
        );

        offer.put("priceValidUntil", "2027-12-31");

        offer.put("seller", Map.of(
                "@id", baseUrl + "/#organization"
        ));

        offer.put("shippingDetails", shippingDetails);

        offer.put("hasMerchantReturnPolicy", returnPolicy);

        productJson.put("offers", offer);

        // ================= REVIEW =================
        if (reviews != null && !reviews.isEmpty()) {

            double avg = reviews.stream()
                    .mapToInt(ReviewResponse::getRating)
                    .average()
                    .orElse(5);

            Map<String, Object> aggregateRating = new LinkedHashMap<>();

            aggregateRating.put("@type", "AggregateRating");

            aggregateRating.put(
                    "ratingValue",
                    String.format("%.1f", avg)
            );

            aggregateRating.put("reviewCount", reviews.size());

            aggregateRating.put("bestRating", "5");

            aggregateRating.put("worstRating", "1");

            productJson.put("aggregateRating", aggregateRating);

            List<Map<String, Object>> reviewList = reviews.stream()
                    .limit(5)
                    .map(r -> {

                        Map<String, Object> review = new LinkedHashMap<>();

                        review.put("@type", "Review");

                        review.put("author", Map.of(
                                "@type", "Person",
                                "name",
                                r.getReviewName() != null
                                        ? r.getReviewName()
                                        : "Khách hàng"
                        ));

                        review.put(
                                "reviewBody",
                                r.getContent() != null
                                        ? r.getContent()
                                        : ""
                        );

                        review.put("reviewRating", Map.of(
                                "@type", "Rating",
                                "ratingValue", r.getRating(),
                                "bestRating", "5",
                                "worstRating", "1"
                        ));

                        return review;
                    })
                    .toList();

            productJson.put("review", reviewList);
        }

        // ================= ROOT =================
        Map<String, Object> root = new LinkedHashMap<>();

        root.put("@context", "https://schema.org");

        root.put("@graph", List.of(
                place,
                organization,
                website,
                breadcrumb,
                webpage,
                productJson
        ));

        String jsonLd = mapper.writeValueAsString(root);

        model.addAttribute("jsonLd", jsonLd);

        return "client/product/detail";
    }

    @GetMapping("gio-hang")
    public String cart(Model model) {


        model.addAttribute("titlePage", "Giỏ hàng");

        return "client/cart";
    }
}
