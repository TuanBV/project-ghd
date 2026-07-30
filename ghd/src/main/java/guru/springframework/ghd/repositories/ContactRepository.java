package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.Contact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {
    Page<Contact> findAllByFullNameContainingIgnoreCaseOrPhoneContaining(String fullName, String phone, Pageable pageable);

    @Query(value="SELECT u.* FROM contact u WHERE u.del_flag = 0", nativeQuery = true)
    Page<Contact> findAll(Pageable pageable);

    @Query(value="SELECT u.* FROM contact u WHERE u.id = :contactId and u.del_flag = 0", nativeQuery = true)
    Optional<Contact> findById(@Param("contactId") String contactId);
}
