package org.revature.revconnect.repository;

import org.revature.revconnect.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.privacy = 'PUBLIC' ORDER BY p.createdAt DESC")
    Page<Post> findPublicPosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdIn(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :tag, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByContentContainingTag(@Param("tag") String tag, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String query, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.privacy = 'PUBLIC' AND " +
            "(:query IS NULL OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:author IS NULL OR LOWER(p.user.username) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
            "(:postType IS NULL OR p.postType = :postType) AND " +
            "(:minLikes IS NULL OR p.likeCount >= :minLikes) AND " +
            "(:dateFrom IS NULL OR p.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR p.createdAt <= :dateTo) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("query") String query,
                           @Param("author") String author,
                           @Param("postType") org.revature.revconnect.enums.PostType postType,
                           @Param("minLikes") Integer minLikes,
                           @Param("dateFrom") java.time.LocalDateTime dateFrom,
                           @Param("dateTo") java.time.LocalDateTime dateTo,
                           Pageable pageable);

    List<Post> findByUserIdAndPinnedTrueOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findByUserIdWithPinnedFirst(@Param("userId") Long userId, Pageable pageable);
}
