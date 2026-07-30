package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.Slider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface SliderRepository extends JpaRepository<Slider, UUID> {

    Page<Slider> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query(value = "SELECT s.* FROM slider s WHERE s.del_flag = 0", nativeQuery = true)
    Page<Slider> findAllActive(Pageable pageable);

    @Query(value = "SELECT s.* FROM slider s WHERE s.id = :sliderId AND s.del_flag = 0", nativeQuery = true)
    Optional<Slider> findActiveById(@Param("sliderId") UUID sliderId);

    Page<Slider> findByDelFlag(Integer flag, Pageable pageable);

    List<Slider> findAllByDelFlag(Integer flag);
}