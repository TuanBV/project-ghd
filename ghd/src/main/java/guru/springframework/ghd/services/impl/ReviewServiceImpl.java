package guru.springframework.ghd.services.impl;

import guru.springframework.ghd.dto.review.IReviewResponse;
import guru.springframework.ghd.dto.review.ReviewRequest;
import guru.springframework.ghd.dto.review.ReviewResponse;
import guru.springframework.ghd.entities.Product;
import guru.springframework.ghd.entities.ProductImage;
import guru.springframework.ghd.entities.Review;
import guru.springframework.ghd.entities.ReviewImage;
import guru.springframework.ghd.mappers.ReviewMapper;
import guru.springframework.ghd.repositories.ReviewRepository;
import guru.springframework.ghd.repositories.ReviewImageRepository;
import guru.springframework.ghd.services.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import static guru.springframework.ghd.utils.UploadImageUtil.handleImageUpload;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewMapper reviewMapper;

    private final String SUB_FOLDER = "reviews";

    @Override
    public List<ReviewResponse> getByProductId(String productId) {
        List<IReviewResponse> data = reviewRepository.findByProductId(productId);

        // Get image by review
        List<String> idList = data.stream().map(IReviewResponse::getId).collect(Collectors.toList());
        List<ReviewImage> images = reviewImageRepository.findByReviewId(idList);

        return data.stream().map(r -> {
            ReviewResponse res = new ReviewResponse();
            res.setId(r.getId());
            res.setProductId(r.getProductId());
            res.setRating(r.getRating());
            res.setContent(r.getContent());
            res.setReviewName(r.getReviewName());
            List<ReviewImage> imgList = images.stream().filter(item -> item.getReviewId().equals(r.getId())).collect(Collectors.toList());
            res.setImages(imgList);

            return res;
        }).collect(Collectors.toList());
    }

    @Override
    public ReviewResponse create(ReviewRequest request, List<MultipartFile> images) {
        Review entity = reviewMapper.toEntity(request);
        final Review savedReview = reviewRepository.save(entity);

        // Handle save image list of review
        List<ReviewImage> savedImages = new ArrayList<>();
        if (images != null) {
            List<ReviewImage> imageList = images.stream()
                    .filter(file -> !file.isEmpty())
                    .map(file -> {
                        ReviewImage imgEntity = new ReviewImage();
                            imgEntity.setImageUrl(handleImageUpload(file, SUB_FOLDER));
                        imgEntity.setReviewId(savedReview.getId().toString());
                        return imgEntity;
                    })
                    .collect(Collectors.toList());

            if (!imageList.isEmpty()) {
                reviewImageRepository.saveAll(imageList);
            }
        }

        ReviewResponse reviewResponse = reviewMapper.reviewToReviewResponse(savedReview);
        reviewResponse.setImages(savedImages);
        return reviewResponse;
    }
}