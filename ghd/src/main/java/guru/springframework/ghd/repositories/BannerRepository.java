package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    Page<Banner> findAllByTitleContainingIgnoreCaseAndDelFlag(String title, Integer delFlag, Pageable pageable);

    Page<Banner> findByDelFlag(Integer delFlag, Pageable pageable);

    List<Banner> findAllByDelFlag(Integer delFlag);

    @Query(value = "SELECT b.* FROM banner b WHERE b.del_flag = 0", nativeQuery = true)
    Page<Banner> findAllActive(Pageable pageable);

    @Query(value = "SELECT b.* FROM banner b WHERE b.id = :bannerId AND b.del_flag = 0", nativeQuery = true)
    Optional<Banner> findActiveById(@Param("bannerId") UUID bannerId);

    // Trong BannerRepository.java
    Optional<Banner> findByPositionAndIsActiveAndDelFlag(String position, Integer isActive, Integer delFlag);
}