package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.review.ReviewRequest;
import guru.springframework.ghd.dto.review.ReviewResponse;
import guru.springframework.ghd.entities.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    Review reviewResponseToReview(ReviewResponse reviewResponse);

    ReviewResponse reviewToReviewResponse(Review review);

    Review toEntity(ReviewRequest request);
}