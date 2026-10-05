package com.princekumar.itams.user;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);

    /** Loads the person with each account; used to show names instead of user ids. */
    @EntityGraph(attributePaths = "person")
    List<UserAccount> findByIdIn(Collection<Long> ids);
}
