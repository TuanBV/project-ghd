package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ReviewImageRepository extends JpaRepository<ReviewImage, UUID> {

    @Query(value = "SELECT ri.* FROM review_image ri WHERE ri.review_id in (:reviewIdList) AND ri.del_flag = 0", nativeQuery = true)
    List<ReviewImage> findByReviewId(List<String> reviewIdList);
}