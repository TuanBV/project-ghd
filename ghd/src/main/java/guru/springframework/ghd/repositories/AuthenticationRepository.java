package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface AuthenticationRepository extends JpaRepository<User, UUID> {

    @Query(value="SELECT u.* FROM user u WHERE u.username = :username and u.del_flag = 0", nativeQuery = true)
    Optional<User> findByUsername(String username);
}
