package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stories", description = "Story Management APIs")
public class StoryController {

    @PostMapping
    @Operation(summary = "Create a new story")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createStory(
            @RequestParam String mediaUrl,
            @RequestParam(required = false) String caption) {
        log.info("Creating new story");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Story created", Map.of("storyId", 1L)));
    }

    @GetMapping
    @Operation(summary = "Get current user's stories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyStories() {
        log.info("Getting my stories");
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/feed")
    @Operation(summary = "Get stories feed from followed users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoriesFeed() {
        log.info("Getting stories feed");
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get stories of a specific user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserStories(@PathVariable Long userId) {
        log.info("Getting stories for user: {}", userId);
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "Get a specific story")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStory(@PathVariable Long storyId) {
        log.info("Getting story: {}", storyId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("id", storyId)));
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete a story")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable Long storyId) {
        log.info("Deleting story: {}", storyId);
        return ResponseEntity.ok(ApiResponse.success("Story deleted", null));
    }

    @PostMapping("/{storyId}/view")
    @Operation(summary = "Mark story as viewed")
    public ResponseEntity<ApiResponse<Void>> viewStory(@PathVariable Long storyId) {
        log.info("Viewing story: {}", storyId);
        return ResponseEntity.ok(ApiResponse.success("Story viewed", null));
    }

    @GetMapping("/{storyId}/viewers")
    @Operation(summary = "Get story viewers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoryViewers(@PathVariable Long storyId) {
        log.info("Getting viewers for story: {}", storyId);
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/{storyId}/react")
    @Operation(summary = "React to a story")
    public ResponseEntity<ApiResponse<Void>> reactToStory(
            @PathVariable Long storyId,
            @RequestParam String reaction) {
        log.info("Reacting to story {} with {}", storyId, reaction);
        return ResponseEntity.ok(ApiResponse.success("Reaction added", null));
    }

    @PostMapping("/{storyId}/reply")
    @Operation(summary = "Reply to a story")
    public ResponseEntity<ApiResponse<Void>> replyToStory(
            @PathVariable Long storyId,
            @RequestParam String message) {
        log.info("Replying to story {}", storyId);
        return ResponseEntity.ok(ApiResponse.success("Reply sent", null));
    }

    @GetMapping("/highlights")
    @Operation(summary = "Get story highlights")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHighlights() {
        log.info("Getting highlights");
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/highlights")
    @Operation(summary = "Create a highlight from stories")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createHighlight(
            @RequestParam String name,
            @RequestParam List<Long> storyIds) {
        log.info("Creating highlight: {}", name);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Highlight created", Map.of("highlightId", 1L)));
    }

    @DeleteMapping("/highlights/{highlightId}")
    @Operation(summary = "Delete a highlight")
    public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable Long highlightId) {
        log.info("Deleting highlight: {}", highlightId);
        return ResponseEntity.ok(ApiResponse.success("Highlight deleted", null));
    }

    @GetMapping("/archive")
    @Operation(summary = "Get archived stories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getArchivedStories() {
        log.info("Getting archived stories");
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }
}
