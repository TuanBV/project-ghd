package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.SysParam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface SysParamRepository extends JpaRepository<SysParam, Long> {
    Optional<SysParam> findByParamKey(String key);

    @Query("SELECT s FROM SysParam s WHERE " +
            "(:groupCode IS NULL OR s.groupCode = :groupCode)")
    List<SysParam> findAllByFilters(@Param("groupCode") String groupCode);
}
