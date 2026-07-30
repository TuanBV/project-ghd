package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface UserImagesRepository extends JpaRepository<UserImage, UUID> {
    @Query("SELECT MAX(ui.imageOrder) FROM UserImage ui WHERE ui.userId = :userId")
    Integer findMaxOrderSetByUserId(@Param("userId") String userId);
}
