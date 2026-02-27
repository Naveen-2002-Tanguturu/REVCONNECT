package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.HashtagRepository;
import org.revature.revconnect.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
@Slf4j
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final AuthService authService;

    @Transactional
    public void createOrIncrement(String name) {
        String normalizedName = normalizeHashtag(name);
        log.info("Processing hashtag: {}", normalizedName);

        Optional<Hashtag> existingHashtag = hashtagRepository.findByName(normalizedName);

        if (existingHashtag.isPresent()) {
            Hashtag hashtag = existingHashtag.get();
            hashtag.incrementUsage();
            hashtagRepository.save(hashtag);
        } else {
            Hashtag newHashtag = Hashtag.builder()
                    .name(normalizedName)
                    .build();
            hashtagRepository.save(newHashtag);
        }
    }

    public List<Hashtag> getTrending(int limit) {
        log.info("Fetching top {} trending hashtags", limit);
        return hashtagRepository.findTrending(PageRequest.of(0, limit));
    }

    public Hashtag getHashtag(String name) {
        String normalizedName = normalizeHashtag(name);
        return hashtagRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Hashtag", "name", normalizedName));
    }

    public PagedResponse<PostResponse> getPostsByHashtag(String name, int page, int size) {
        String normalizedName = normalizeHashtag(name);
        String tag = "#" + normalizedName;
        Page<Post> posts = postRepository.findByContentContainingTag(tag, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    public List<Hashtag> search(String query) {
        log.info("Searching hashtags with query: {}", query);
        return hashtagRepository.findByNameContainingIgnoreCase(query);
    }

    public List<Hashtag> getSuggestedHashtags(int limit) {
        return getTrending(limit);
    }

    public List<Map<String, Object>> getFollowedHashtagsView() {
        User currentUser = authService.getCurrentUser();
        List<Post> posts = postRepository.findByUserId(currentUser.getId());
        Map<String, Integer> counts = new HashMap<>();

        for (Post post : posts) {
            if (post.getContent() == null) {
                continue;
            }
            String[] words = post.getContent().split("\\s+");
            for (String word : words) {
                if (word.startsWith("#") && word.length() > 1) {
                    String normalized = normalizeHashtag(word);
                    counts.put(normalized, counts.getOrDefault(normalized, 0) + 1);
                }
            }
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(20)
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("tag", entry.getKey());
                    map.put("usageCount", entry.getValue());
                    return map;
                })
                .toList();
    }

    private String normalizeHashtag(String name) {
        if (name == null)
            return "";
        String normalized = name.toLowerCase().trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Transactional
    public void processHashtagsFromContent(String content) {
        if (content == null)
            return;

        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.startsWith("#") && word.length() > 1) {
                createOrIncrement(word);
            }
        }
    }
}
