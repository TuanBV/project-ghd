package guru.springframework.ghd.dto.review;

import guru.springframework.ghd.entities.ReviewImage;
import lombok.Data;

import java.util.*;

@Data
public class ReviewResponse {
    private String id;
    private String productId;
    private Integer rating;
    private String content;
    private String reviewName;
    private List<ReviewImage> images;
}