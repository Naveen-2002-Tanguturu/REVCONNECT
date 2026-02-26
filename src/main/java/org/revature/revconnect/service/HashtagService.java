package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.model.Post;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;

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
