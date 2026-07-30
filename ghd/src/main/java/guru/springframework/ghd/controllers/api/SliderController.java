package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.constants.DefaultPage;
import guru.springframework.ghd.dto.slider.SliderRequest;
import guru.springframework.ghd.dto.slider.SliderResponse;
import guru.springframework.ghd.services.SliderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/slider")
public class SliderController extends BaseController {

    private final SliderService sliderService;

    @GetMapping
    public ResponseEntity<?> getSliders(
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = DefaultPage.PAGE_STRING) Integer pageNumber,
            @RequestParam(required = false, defaultValue = DefaultPage.SIZE_CLIENT_STRING) Integer pageSize,
            @RequestParam(required = false, defaultValue = DefaultPage.POSITION) String sortField,
            @RequestParam(required = false, defaultValue = DefaultPage.ASC) String sortDir
    ) {
        Page<SliderResponse> sliderPage = sliderService.getList(title, sortField, sortDir, pageNumber, pageSize);
        return ok(sliderPage);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addSlider(@Valid @ModelAttribute SliderRequest sliderRequest) {
        SliderResponse savedSlider = sliderService.addSlider(sliderRequest);
        return ok(savedSlider);
    }

    @GetMapping("/{sliderId}")
    public ResponseEntity<?> getById(@PathVariable("sliderId") String sliderId) {
        return sliderService.getById(sliderId)
                .map(this::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping(value = "/{sliderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateById(@PathVariable("sliderId") String sliderId,
                                        @Valid @ModelAttribute SliderRequest sliderRequest) {
        Optional<SliderResponse> updatedSlider = sliderService.updateById(sliderId, sliderRequest);

        if (updatedSlider.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ok(updatedSlider.get());
    }

    @DeleteMapping("/{sliderId}")
    public ResponseEntity<?> deleteById(@PathVariable("sliderId") String sliderId) {
        sliderService.deleteById(sliderId);
        return ok("Deleted successfully");
    }
}