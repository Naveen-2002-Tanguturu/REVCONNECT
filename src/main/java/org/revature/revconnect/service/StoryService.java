package org.revature.revconnect.service;

import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.StoryRepository;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoryService {

    private final StoryRepository storyRepository;
    private final AuthService authService;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Story createStory(String mediaUrl, String caption) {
        User currentUser = authService.getCurrentUser();
        log.info("Creating story for user: {}", currentUser.getUsername());

        Story story = Story.builder()
                .user(currentUser)
                .mediaUrl(mediaUrl)
                .caption(caption)
                .build();

        Story savedStory = storyRepository.save(story);
        log.info("Story created with ID: {}", savedStory.getId());
        return savedStory;
    }

    public List<Story> getActiveStories(User user) {
        log.info("Fetching active stories for user: {}", user.getUsername());
        return storyRepository.findByUserAndExpiresAtAfterOrderByCreatedAtDesc(user, LocalDateTime.now());
    }

    public List<Story> getStoriesFeed(List<User> followedUsers) {
        log.info("Fetching stories feed for {} users", followedUsers.size());
        return storyRepository.findActiveStoriesByUsers(followedUsers, LocalDateTime.now());
    }

    public List<Story> getStoriesFeedForCurrentUser() {
        User currentUser = authService.getCurrentUser();
        List<Long> followingIds = connectionRepository.findFollowingUserIds(currentUser.getId());
        List<User> followedUsers = userRepository.findAllById(followingIds);
        return getStoriesFeed(followedUsers);
    }

    public Story getStory(Long storyId) {
        return storyRepository.findById(storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story", "id", storyId));
    }

    public List<Story> getHighlights(User user) {
        log.info("Fetching highlights for user: {}", user.getUsername());
        return storyRepository.findByUserAndIsHighlightTrueOrderByCreatedAtDesc(user);
    }

    @Transactional
    public Story markAsHighlight(Long storyId) {
        Story story = getStory(storyId);

        story.setHighlight(true);
        log.info("Story {} marked as highlight", storyId);
        return storyRepository.save(story);
    }

    @Transactional
    public Story unmarkHighlight(Long storyId) {
        Story story = getStory(storyId);
        story.setHighlight(false);
        log.info("Story {} unmarked as highlight", storyId);
        return storyRepository.save(story);
    }

    @Transactional
    public void incrementViewCount(Long storyId) {
        Story story = getStory(storyId);

        story.setViewCount(story.getViewCount() + 1);
        storyRepository.save(story);
    }

    public List<Story> getArchivedStories(User user) {
        log.info("Fetching archived stories for user: {}", user.getUsername());
        return storyRepository.findByUserAndExpiresAtBeforeOrderByCreatedAtDesc(user, LocalDateTime.now());
    }

    @Transactional
    public void deleteStory(Long storyId) {
        User currentUser = authService.getCurrentUser();
        Story story = getStory(storyId);

        if (!story.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only delete your own stories");
        }

        storyRepository.delete(story);
        log.info("Story {} deleted", storyId);
    }

    @Transactional
    public void cleanupExpiredStories() {
        List<Story> expiredStories = storyRepository.findExpiredStories(LocalDateTime.now());
        log.info("Cleaning up {} expired stories", expiredStories.size());
        storyRepository.deleteAll(expiredStories);
    }
}
