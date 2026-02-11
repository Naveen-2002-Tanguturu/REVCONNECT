package org.revature.revconnect.Repository;
import com.revconnect.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

    Optional<Share> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    long countByPostId(Long postId);
}