package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.brand.BrandRequest;
import guru.springframework.ghd.dto.brand.BrandResponse;
import guru.springframework.ghd.services.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brand")
public class BrandController extends BaseController {

    private final BrandService brandService;

    @GetMapping
    public ResponseEntity<?> getCategories(
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = DefaultPage.PAGE_STRING) Integer pageNumber,
            @RequestParam(required = false, defaultValue = DefaultPage.SIZE_CLIENT_STRING) Integer pageSize,
            @RequestParam(required = false, defaultValue = DefaultPage.ID) String sortField,
            @RequestParam(required = false, defaultValue = DefaultPage.DESC) String sortDir
    ) {
        Page<BrandResponse> brandPage = brandService.getList(title, sortField, sortDir, pageNumber, pageSize);
        return ok(brandPage);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addBrand(@Valid @ModelAttribute BrandRequest brandRequest) {
        BrandResponse savedBrand = brandService.addBrand(brandRequest);
        return ok(savedBrand);
    }

    @GetMapping("/{brandId}")
    public ResponseEntity<?> getById(@PathVariable("brandId") String brandId) {
        return brandService.getById(brandId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{brandId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateById(@PathVariable("brandId") String brandId,
                                     @Valid @ModelAttribute BrandRequest brandRequest) {
        Optional<BrandResponse> updatedBrand = brandService.updateById(brandId, brandRequest);

        if (updatedBrand.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ok(updatedBrand.get());
    }

    @DeleteMapping("/{brandId}")
    public ResponseEntity<?> deleteById(@PathVariable("brandId") String brandId) {
        brandService.deleteById(brandId);
        return ok("Deleted successfully");
    }
}
