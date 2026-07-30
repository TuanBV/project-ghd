package guru.springframework.ghd.dto.review;

import lombok.Data;

@Data
public class ReviewRequest {
    private String productId;
    private Integer rating;
    private String content;
    private String reviewName;
    private String reviewEmail;
}