package guru.springframework.ghd.controllers.views;

import guru.springframework.ghd.constants.enums.DelFlag;
import guru.springframework.ghd.dto.NavBarResponse;
import guru.springframework.ghd.dto.brand.BrandResponse;
import guru.springframework.ghd.dto.category.CategoryResponse;
import guru.springframework.ghd.dto.product.ProductResponse;
import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.services.BrandService;
import guru.springframework.ghd.services.CategoryService;
import guru.springframework.ghd.services.ProductService;
import guru.springframework.ghd.services.SysParamService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.*;
import java.util.stream.Collectors;

// Scoped to controllers.views only - this used to be a bare @ControllerAdvice, which
// Spring MVC applies to every controller including @RestController API endpoints. That
// meant every single API call (JSON, never rendering a Thymeleaf view) was paying for a
// full categories/brands/products table load just to build sidebar nav data it never
// used - and, more seriously, it was loading every Product row into the request's
// Hibernate persistence context before service-layer code got a chance to run its own
// (correctly locked) query for the same row, so a later `SELECT ... FOR UPDATE` could be
// handed back the stale, already-cached entity instead of the freshly locked one.
@ControllerAdvice(basePackages = "guru.springframework.ghd.controllers.views")
@RequiredArgsConstructor
public class GlobalCommon {
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ProductService productService;
    private final SysParamService sysParamService;

    @ModelAttribute("navBarData")
    public List<NavBarResponse> getSidebarData() {
        List<CategoryResponse> categories = categoryService.getAll().stream().filter(c -> c.getDelFlag().equals(DelFlag.ACTIVE.get())).toList();
        List<BrandResponse> brands = brandService.getAll().stream().filter(c -> c.getDelFlag().equals(DelFlag.ACTIVE.get())).toList();
        List<Product> lstProduct = productService.getAll();

        List<NavBarResponse> list = new ArrayList<>();

        categories.forEach(category -> {
            Set<String> activeBrandIds = lstProduct.stream()
                    .filter(p -> p.getCategoryId().equals(category.getId().toString()))
                    .map(Product::getBrandId)
                    .collect(Collectors.toSet());

            List<BrandResponse> brandsInContent = brands.stream()
                    .filter(brand -> activeBrandIds.contains(brand.getId().toString()))
                    .collect(Collectors.toList());

            if (!brandsInContent.isEmpty()) {
                list.add(NavBarResponse.builder()
                        .id(category.getId())
                        .title(category.getTitle())
                        .slug(category.getSlug())
                        .childs(brandsInContent)
                        .build()
                );
            }
        });


        return list;
    }

//    @ModelAttribute("getAvatar")
//    public String getAvatar(Authentication authentication) {
//        if (authentication != null && authentication.isAuthenticated()) {
//            // Gọi service lấy logo thực tế ở đây
//            return "https://your-domain.com/logo-active.png";
//        }
//        // Trả về mặc định hoặc null nếu chưa login
//        return "default-logo.png";
//    }

    @ModelAttribute("hotline")
    public String getHotline() {
        // Optional#get() without a presence check threw NoSuchElementException for
        // every single request (this @ModelAttribute runs on every controller,
        // including non-view/API/actuator requests) whenever no "hotline" sys_param
        // row exists yet - e.g. a freshly migrated, not-yet-configured database.
        return sysParamService.getByKey("hotline").map(SysParamResponse::getParamValue).orElse(null);
    }

    @ModelAttribute("sysParams")
    public Map<String, String> getAllSysParams() {
        List<SysParamResponse> allParams = sysParamService.getAll();

        return allParams.stream()
                .collect(Collectors.toMap(
                        SysParamResponse::getParamKey,
                        SysParamResponse::getParamValue,
                        (existing, replacement) -> existing // Xử lý nếu trùng key
                ));
    }

    @ModelAttribute("currentUrl")
    public String getCurrentUrl(HttpServletRequest request) {
        return request.getRequestURL().toString();
    }
}