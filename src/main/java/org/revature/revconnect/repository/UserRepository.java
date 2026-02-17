package org.revature.revconnect.repository;

import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.enums.Privacy;
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

    // 🔍 Search by username or name (all users)
    @Query("""
        SELECT u FROM User u
        WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    Page<User> searchByUsernameOrName(@Param("query") String query, Pageable pageable);

    // 🔓 Search only PUBLIC users
    @Query("""
        SELECT u FROM User u
        WHERE u.privacy = :privacy
          AND (
                LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))
          )
    """)
    Page<User> searchPublicUsers(@Param("query") String query,
                                 @Param("privacy") Privacy privacy,
                                 Pageable pageable);

    // ⭐ Suggested users (exclude current user, only PUBLIC)
    @Query("""
        SELECT u FROM User u
        WHERE u.id <> :currentUserId
          AND u.privacy = :privacy
        ORDER BY u.createdAt DESC
    """)
    Page<User> findSuggestedUsers(@Param("currentUserId") Long currentUserId,
                                  @Param("privacy") Privacy privacy,
                                  Pageable pageable);

    // 🤝 Mutual connections
    @Query("""
        SELECT DISTINCT f FROM Connection c1
        JOIN Connection c2 ON c1.following = c2.following
        JOIN User f ON f.id = c1.following.id
        WHERE c1.follower.id = :userId1
          AND c2.follower.id = :userId2
          AND c1.status = :status
          AND c2.status = :status
    """)
    Page<User> findMutualConnections(@Param("userId1") Long userId1,
                                     @Param("userId2") Long userId2,
                                     @Param("status") ConnectionStatus status,
                                     Pageable pageable);
}
