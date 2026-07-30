package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.banner.BannerRequest;
import guru.springframework.ghd.dto.banner.BannerResponse;
import guru.springframework.ghd.services.BannerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/banner")
public class BannerController extends BaseController {

    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<?> getBanners(
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = DefaultPage.PAGE_STRING) Integer pageNumber,
            @RequestParam(required = false, defaultValue = DefaultPage.SIZE_CLIENT_STRING) Integer pageSize,
            @RequestParam(required = false, defaultValue = DefaultPage.POSITION) String sortField,
            @RequestParam(required = false, defaultValue = DefaultPage.ASC) String sortDir
    ) {
        Page<BannerResponse> bannerPage = bannerService.getList(title, sortField, sortDir, pageNumber, pageSize);
        return ok(bannerPage);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addBanner(@Valid @ModelAttribute BannerRequest bannerRequest) {
        BannerResponse savedBanner = bannerService.addBanner(bannerRequest);
        return ok(savedBanner);
    }

    @GetMapping("/{bannerId}")
    public ResponseEntity<?> getById(@PathVariable("bannerId") UUID bannerId) {
        return bannerService.getById(bannerId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{bannerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateById(@PathVariable("bannerId") UUID bannerId,
                                        @Valid @ModelAttribute BannerRequest bannerRequest) {
        Optional<BannerResponse> updatedBanner = bannerService.updateById(bannerId, bannerRequest);

        if (updatedBanner.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ok(updatedBanner.get());
    }

    @DeleteMapping("/{bannerId}")
    public ResponseEntity<?> deleteById(@PathVariable("bannerId") UUID bannerId) {
        bannerService.deleteById(bannerId);
        return ok("Deleted successfully");
    }
}