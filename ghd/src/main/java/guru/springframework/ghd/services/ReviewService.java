package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.review.ReviewRequest;
import guru.springframework.ghd.dto.review.ReviewResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

public interface ReviewService {
    List<ReviewResponse> getByProductId(String productId);
    ReviewResponse create(ReviewRequest request, List<MultipartFile>  images);
}
