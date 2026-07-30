package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    @Query(value = "SELECT p.* FROM policy p " +
            "WHERE (:name IS NULL OR p.package_name LIKE CONCAT('%', :name, '%')) " +
            "AND p.del_flag = 0 " +
            "ORDER BY " +
            "CASE WHEN :sort = 'asc' THEN " +
            "  CASE WHEN :field = 'packageName' THEN p.package_name END " +
            "END ASC, " +
            "CASE WHEN :sort = 'desc' THEN " +
            "  CASE WHEN :field = 'packageName' THEN p.package_name END " +
            "END DESC, " +
            "p.created_date DESC", nativeQuery = true)
    List<Policy> findListCustom(@Param("name") String name, @Param("field") String field, @Param("sort") String sort);
}