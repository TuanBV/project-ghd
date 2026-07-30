package guru.springframework.ghd.dto.review;

public interface IReviewResponse {
    String getId();
    String getProductId();
    String getReviewName();
    String getContent();
    Integer getRating();
}