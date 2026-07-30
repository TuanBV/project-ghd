package guru.springframework.ghd.controllers.views;

import guru.springframework.ghd.dto.banner.BannerResponse;
import guru.springframework.ghd.dto.news.NewsResponse;
import guru.springframework.ghd.dto.product.ProductDetailResponse;
import guru.springframework.ghd.dto.slider.SliderResponse;
import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import guru.springframework.ghd.dto.user.UserResponse;
import guru.springframework.ghd.services.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/v1/")
public class ViewAdminController {

    private final UserService userService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final SysParamService sysParamService;
    private final ProductService productService;
    private final NewsService newsService;
    private final SliderService sliderService;
    private final BannerService bannerService;
    private final PolicyService policyService;

    @GetMapping("")
    public String home(Model model) {
        model.addAttribute("titlePage", "Dashboard");
        model.addAttribute("activePage", "dashboard");
        return "admin/home";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("user")
    public String user(Model model) {
        model.addAttribute("titlePage", "User manager");
        model.addAttribute("activePage", "user");
        model.addAttribute("users", userService.getList(null, null, null));

        return "admin/user";
    }

    @GetMapping("category")
    public String category(Model model) {
        model.addAttribute("titlePage", "Quản lý danh mục");
        model.addAttribute("activePage", "category");
        model.addAttribute("categories", categoryService.getList(null, null, null ,null, null));

        return "admin/category";
    }

    @GetMapping("brand")
    public String brand(Model model) {
        model.addAttribute("titlePage", "Brand manager");
        model.addAttribute("activePage", "brand");
        model.addAttribute("brands", brandService.getList(null,null,null, null, null));

        return "admin/brand";
    }

    @GetMapping("product")
    public String product(Model model) {
        model.addAttribute("titlePage", "Quản lý sản phẩm");
        model.addAttribute("activePage", "product");

        // Chỉ lấy dữ liệu cho bộ lọc Dropdown
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("brands", brandService.getAll());

        return "admin/product/list";
    }

    @GetMapping("product/preview")
    public String previewProduct(Model model) {
        model.addAttribute("titlePage", "Chế độ xem trước");
        return "admin/product/preview";
    }

    @GetMapping({"product/add", "product/edit/{id}"})
    public String addOrUpdateProduct(@PathVariable(required = false) String id, Model model) {
        ProductDetailResponse product = new ProductDetailResponse();
        String titlePage;

        if (id != null) {
            product = productService.getById(id); // Giả định bạn có ProductService
            titlePage = "Cập nhật sản phẩm";
        } else {
            titlePage = "Thêm mới sản phẩm";
        }

        model.addAttribute("product", product);
        model.addAttribute("titlePage", titlePage);
        model.addAttribute("activePage", "product");
        model.addAttribute("isDuplicate", false);

        // Cần thiết để chọn cho sản phẩm
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("brands", brandService.getAll());
        model.addAttribute("policies", policyService.getAll());
        return "admin/product/detail";
    }

    @GetMapping("product/duplicate/{id}")
    public String duplicateProduct(@PathVariable String id, Model model) {
        ProductDetailResponse product = productService.getById(id);

        product.setId(null);

        product.setTitle(product.getTitle() + " - Copy");

        model.addAttribute("product", product);
        model.addAttribute("titlePage", "Nhân bản sản phẩm");
        model.addAttribute("activePage", "product");
        model.addAttribute("isDuplicate", true);
        model.addAttribute("duplicateSourceId", id);

        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("brands", brandService.getAll());
        model.addAttribute("policies", policyService.getAll());

        return "admin/product/detail";
    }

    @GetMapping("/slider")
    public String slide(Model model) {
        model.addAttribute("titlePage", "Quản lý Slider");
        model.addAttribute("activePage", "slide");

        Page<SliderResponse> sliderPage = sliderService.getList(null, "position", "asc", 0, 50);

        model.addAttribute("sliders", sliderPage.getContent());

        return "admin/slider";
    }

    @GetMapping("/banner")
    public String banner(Model model) {
        model.addAttribute("titlePage", "Quản lý Banner");
        model.addAttribute("activePage", "banner");

        Page<BannerResponse> bannerPage = bannerService.getList(null, "position", "asc", 0, 50);

        model.addAttribute("banners", bannerPage.getContent());

        return "admin/banner";
    }

    @GetMapping("/policy")
    public String policy(Model model) {
        model.addAttribute("titlePage", "Quản lý chính sách");
        model.addAttribute("activePage", "policy");

        return "admin/policy";
    }

    @GetMapping("news")
    public String news(Model model) {
        model.addAttribute("titlePage", "Quản lý bài viết");
        model.addAttribute("activePage", "news");
        return "admin/news/list";
    }

    @GetMapping("news/preview")
    public String previewNews(Model model) {
        model.addAttribute("titlePage", "Chế độ xem trước");
        return "admin/news/preview";
    }

    @GetMapping({"news/add", "news/edit/{id}"})
    public String addOrUpdateNews(@PathVariable(required = false) String id, Model model) {
        Optional<NewsResponse> news = Optional.empty();
        String titlePage;

        if (id != null) {
            news = newsService.getById(id);
            titlePage = "Cập nhật tin tức";
        } else {
            titlePage = "Thêm mới tin tức";
        }

        model.addAttribute("news", news);
        model.addAttribute("titlePage", titlePage);
        model.addAttribute("activePage", "news");
        model.addAttribute("categories", categoryService.getAll());
        model.addAttribute("brands", brandService.getAll());

        return "admin/news/detail";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("order")
    public String order(Model model) {
        model.addAttribute("titlePage", "Product manager");
        model.addAttribute("activePage", "order");
        model.addAttribute("brands", brandService.getList(null,null,null, null, null));

        return "admin/order";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("contact")
    public String contact(Model model) {
        model.addAttribute("titlePage", "Product manager");
        model.addAttribute("activePage", "contact");
        model.addAttribute("brands", brandService.getList(null,null,null, null, null));

        return "admin/contact";
    }

    @GetMapping("settings")
    public String settings(Model model) {
        model.addAttribute("titlePage", "Setting manager");
        model.addAttribute("activePage", "settings");
        List<SysParamResponse> sysParamResponseList = sysParamService.getAll();

        Map<String, List<SysParamResponse>> groupedParams = sysParamResponseList.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getGroupCode() != null ? dto.getGroupCode() : "OTHERS"
                ));

        model.addAttribute("groupedParams", groupedParams);

        return "admin/settings";
    }

    @GetMapping("profile")
    public String profile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        UserResponse user = userService.getByUsername(currentUsername);

        model.addAttribute("titlePage", "Hồ sơ cá nhân");
        model.addAttribute("activePage", "settings");
        model.addAttribute("user", user);

        return "admin/profile";
    }

    @GetMapping("sign-in")
    public String signIn(HttpServletRequest request, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/admin/v1/ ";
        }

        return "admin/auth/sign-in";
    }
}
