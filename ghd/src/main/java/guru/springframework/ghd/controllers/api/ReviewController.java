package guru.springframework.ghd.controllers.api;

import guru.springframework.ghd.dto.review.ReviewRequest;
import guru.springframework.ghd.dto.review.ReviewResponse;
import guru.springframework.ghd.services.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/review")
public class ReviewController extends BaseController {

    private final ReviewService reviewService;

    @GetMapping("/list")
    public ResponseEntity<?> getReviews(@RequestParam String productId) {
        List<ReviewResponse> reviews = reviewService.getByProductId(productId);
        return ok(reviews);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReview(
            @RequestPart("data") @Valid ReviewRequest reviewRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        try {
            ReviewResponse review = reviewService.create(reviewRequest, images);
            return ok(review);
        } catch (Exception e) {
            return ng(e.getMessage());
        }
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
//        reviewService.delete(id);
//        return ok("Deleted");
//    }
}