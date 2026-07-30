package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface BrandRepository extends JpaRepository<Brand, UUID> {
    Page<Brand> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query(value="SELECT u.* FROM brand u WHERE u.del_flag = 0", nativeQuery = true)
    Page<Brand> findAll(Pageable pageable);

    @Query(value="SELECT u.* FROM brand u WHERE u.id = :brandId and u.del_flag = 0", nativeQuery = true)
    Optional<Brand> findById(@Param("brandId") String brandId);

    @Query(value = "SELECT c.* FROM brand c WHERE c.del_flag = 0", nativeQuery = true)
    List<Brand> getAll();

    @Query(value = "SELECT c.* FROM brand c WHERE c.del_flag = 0 and c.logo is not null", nativeQuery = true)
    List<Brand> getAllNotNullLogo();
}
