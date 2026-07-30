package guru.springframework.ghd.repositories;

import com.fasterxml.jackson.annotation.JsonIgnore;
import guru.springframework.ghd.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Page<User> findAllByUsernameIsLikeIgnoreCase(String username, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.delFlag = 0")
    Page<User> findAll(Pageable pageable);

    @Query(value="SELECT u.* FROM user u WHERE u.id = :userId and u.del_flag = 0", nativeQuery = true)
    Optional<User> findById(@Param("userId") String userId);

    Optional<User> findByUsername(String username);
}
