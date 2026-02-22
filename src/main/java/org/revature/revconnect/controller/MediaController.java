package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "Media Upload and Management APIs")
public class MediaController {

    @PostMapping("/upload")
    @Operation(summary = "Upload a single file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading file: {}", file.getOriginalFilename());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded", Map.of("url", "/uploads/example.jpg")));
    }

    @PostMapping("/upload/multiple")
    @Operation(summary = "Upload multiple files")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files) {
        log.info("Uploading {} files", files.size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded", List.of()));
    }

    @PostMapping("/upload/profile-picture")
    @Operation(summary = "Upload profile picture")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading profile picture");
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated", Map.of("url", "/uploads/profile.jpg")));
    }

    @PostMapping("/upload/cover-photo")
    @Operation(summary = "Upload cover photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCoverPhoto(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading cover photo");
        return ResponseEntity.ok(ApiResponse.success("Cover photo updated", Map.of("url", "/uploads/cover.jpg")));
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete a media file")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable Long mediaId) {
        log.info("Deleting media: {}", mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media deleted", null));
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMedia(@PathVariable Long mediaId) {
        log.info("Getting media: {}", mediaId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("id", mediaId)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my media files")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting my media");
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/upload/video")
    @Operation(summary = "Upload video")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadVideo(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading video");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Video uploaded", Map.of("url", "/uploads/video.mp4")));
    }

    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Get media thumbnail")
    public ResponseEntity<ApiResponse<Map<String, String>>> getThumbnail(@PathVariable Long mediaId) {
        log.info("Getting thumbnail for media: {}", mediaId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", "/thumbnails/example.jpg")));
    }

    @PostMapping("/{mediaId}/process")
    @Operation(summary = "Process media (resize, compress)")
    public ResponseEntity<ApiResponse<Map<String, String>>> processMedia(
            @PathVariable Long mediaId,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Integer quality) {
        log.info("Processing media: {}", mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media processed", Map.of()));
    }
}
