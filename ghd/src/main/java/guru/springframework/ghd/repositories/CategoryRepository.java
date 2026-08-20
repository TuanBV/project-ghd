package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findAllByTitleContainingIgnoreCase(String title, Pageable pageable);

    // JPQL (không phải native) - native query cũ (+ Pageable có Sort) khiến Spring Data
    // JPA tự chèn ORDER BY vào 1 chuỗi SQL nó không parse được, ném lỗi 500 trên mọi
    // request (cùng lớp bug với OrdersRepository.search, xem OrdersRepository.java).
    @Query("SELECT c FROM Category c WHERE c.delFlag = 0")
    Page<Category> findAll(Pageable pageable);

    @Query(value="SELECT u.* FROM category u WHERE u.id = :categoryId and u.del_flag = 0", nativeQuery = true)
    Optional<Category> findById(@Param("categoryId") String categoryId);

    @Query(value = "SELECT c.* FROM category c WHERE c.del_flag = 0 and c.logo is not null", nativeQuery = true)
    List<Category> findAllNotNullLogo();

    List<Category> findAllByOrderByPriorityAsc();

    @Modifying
    @Query("UPDATE Category c SET c.priority = c.priority + 1 WHERE c.priority >= :newPriority AND c.delFlag = 0")
    void shiftPriorities(@Param("newPriority") Integer newPriority);

    @Query("select coalesce(max(c.priority), 0) from Category c where c.delFlag = 0")
    Integer findMaxPriority();

    @Modifying
    @Query("""
        update Category c
        set c.priority = c.priority + 1
        where c.delFlag = 0
        and c.priority >= :priority
    """)
    void shiftPrioritiesForInsert(Integer priority);

    @Modifying
    @Query("""
    update Category c
    set c.priority = c.priority + 1
    where c.delFlag = 0
    and c.priority >= :newPriority
    and c.priority < :oldPriority
    and c.id <> :categoryId
""")
    void shiftDownWhenMoveUp(String categoryId, Integer oldPriority, Integer newPriority);

    @Modifying
    @Query("""
    update Category c
    set c.priority = c.priority - 1
    where c.delFlag = 0
    and c.priority <= :newPriority
    and c.priority > :oldPriority
    and c.id <> :categoryId
""")
    void shiftUpWhenMoveDown(String categoryId, Integer oldPriority, Integer newPriority);
}
