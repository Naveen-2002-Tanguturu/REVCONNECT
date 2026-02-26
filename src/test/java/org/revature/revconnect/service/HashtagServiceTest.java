package org.revature.revconnect.service;

import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.repository.HashtagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock
    private HashtagRepository hashtagRepository;

    private HashtagService hashtagService;

    private Hashtag testHashtag;

    @BeforeEach
    void setUp() {
        testHashtag = Hashtag.builder()
                .id(1L)
                .name("java")
                .usageCount(5L)
                .lastUsed(LocalDateTime.now())
                .build();

        hashtagService = new HashtagService(hashtagRepository);
    }

    @Test
    void createOrIncrement_NewHashtag() {
        when(hashtagRepository.findByName("java")).thenReturn(Optional.empty());
        when(hashtagRepository.save(any(Hashtag.class))).thenReturn(testHashtag);

        Hashtag result = hashtagService.createOrIncrement("#Java");

        assertNotNull(result);
        assertEquals("java", result.getName());
        verify(hashtagRepository).save(any(Hashtag.class));
    }

    @Test
    void createOrIncrement_ExistingHashtag() {
        when(hashtagRepository.findByName("java")).thenReturn(Optional.of(testHashtag));
        when(hashtagRepository.save(testHashtag)).thenReturn(testHashtag);

        Hashtag result = hashtagService.createOrIncrement("java");

        assertNotNull(result);
        assertEquals(6L, result.getUsageCount());
        verify(hashtagRepository).save(testHashtag);
    }

    @Test
    void getTrending_Success() {
        when(hashtagRepository.findTrending(any(PageRequest.class)))
                .thenReturn(List.of(testHashtag));

        List<Hashtag> trending = hashtagService.getTrending(10);

        assertNotNull(trending);
        assertEquals(1, trending.size());
        assertEquals("java", trending.get(0).getName());
    }

    @Test
    void search_Success() {
        when(hashtagRepository.findByNameContainingIgnoreCase("jav"))
                .thenReturn(List.of(testHashtag));

        List<Hashtag> results = hashtagService.search("jav");

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void processHashtagsFromContent_WithHashtags() {
        when(hashtagRepository.findByName("java")).thenReturn(Optional.of(testHashtag));
        when(hashtagRepository.save(any(Hashtag.class))).thenReturn(testHashtag);

        hashtagService.processHashtagsFromContent("Learning #java and #spring");

        verify(hashtagRepository, atLeast(1)).save(any(Hashtag.class));
    }

    @Test
    void processHashtagsFromContent_NullContent() {
        hashtagService.processHashtagsFromContent(null);

        verify(hashtagRepository, never()).save(any());
    }

    @Test
    void processHashtagsFromContent_NoHashtags() {
        hashtagService.processHashtagsFromContent("No hashtags here");

        verify(hashtagRepository, never()).findByName(any());
    }
}
