package com.princekumar.itams.person;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByIdAndDeletedAtIsNull(Long id);

    /** Case-insensitive uniqueness check among live rows. */
    @Query("""
        SELECT COUNT(p) > 0 FROM Person p
         WHERE LOWER(p.email) = LOWER(:email)
           AND p.deletedAt IS NULL
    """)
    boolean existsByEmailIgnoreCaseLive(String email);

    @Query("""
        SELECT p FROM Person p
         WHERE p.deletedAt IS NULL
           AND (:q IS NULL OR :q = ''
                OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(p.email)     LIKE LOWER(CONCAT('%', :q, '%')))
    """)
    Page<Person> searchLive(String q, Pageable pageable);
}
