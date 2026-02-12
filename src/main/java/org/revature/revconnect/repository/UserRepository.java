package org.revature.revconnect.repository;

import org.revature.revconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    // Search by username or name
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    Page<User> searchByUsernameOrName(
            @Param("query") String query,
            Pageable pageable
    );

    // Suggested users (NO ENUM IN JPQL → SAFE)
    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :currentUserId
        ORDER BY u.createdAt DESC
    """)
    Page<User> findSuggestedUsers(
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );
}
