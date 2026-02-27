package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.revature.revconnect.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Messages", description = "Direct Messaging APIs")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    @Operation(summary = "Get all conversations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting conversations");
        List<Map<String, Object>> conversations = messageService.getConversationPartners().stream()
                .map(this::toUserMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @PostMapping("/conversations")
    @Operation(summary = "Create a new conversation")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createConversation(
            @RequestParam Long recipientId) {
        log.info("Creating conversation with user: {}", recipientId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Conversation created", Map.of("conversationId", recipientId)));
    }

    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Get conversation messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Getting messages for conversation: {}", conversationId);
        Page<Message> messages = messageService.getConversation(conversationId, PageRequest.of(page, size));
        List<Map<String, Object>> response = messages.getContent().stream()
                .map(this::toMessageMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/conversations/{conversationId}")
    @Operation(summary = "Send a message")
    public ResponseEntity<ApiResponse<Map<String, Long>>> sendMessage(
            @PathVariable Long conversationId,
            @RequestParam String content,
            @RequestParam(required = false) String mediaUrl) {
        log.info("Sending message to conversation: {}", conversationId);
        Message message = messageService.sendMessage(conversationId, content, mediaUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Message sent", Map.of("messageId", message.getId())));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Delete a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable Long conversationId) {
        log.info("Deleting conversation: {}", conversationId);
        messageService.deleteConversationWithUser(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation deleted", null));
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete a message")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Long messageId) {
        log.info("Deleting message: {}", messageId);
        messageService.deleteMessage(messageId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted", null));
    }

    @PatchMapping("/messages/{messageId}")
    @Operation(summary = "Edit a message")
    public ResponseEntity<ApiResponse<Void>> editMessage(
            @PathVariable Long messageId,
            @RequestParam String content) {
        log.info("Editing message: {}", messageId);
        messageService.editMessage(messageId, content);
        return ResponseEntity.ok(ApiResponse.success("Message edited", null));
    }

    @PostMapping("/conversations/{conversationId}/read")
    @Operation(summary = "Mark conversation as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long conversationId) {
        log.info("Marking conversation {} as read", conversationId);
        messageService.markAllAsRead(conversationId);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @GetMapping("/unread/count")
    @Operation(summary = "Get unread message count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount() {
        log.info("Getting unread message count");
        int count = (int) messageService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PostMapping("/messages/{messageId}/react")
    @Operation(summary = "React to a message")
    public ResponseEntity<ApiResponse<Void>> reactToMessage(
            @PathVariable Long messageId,
            @RequestParam String reaction) {
        log.info("Reacting to message {} with {}", messageId, reaction);
        return ResponseEntity.ok(ApiResponse.success("Reaction added", null));
    }

    @DeleteMapping("/messages/{messageId}/react")
    @Operation(summary = "Remove reaction from message")
    public ResponseEntity<ApiResponse<Void>> removeReaction(@PathVariable Long messageId) {
        log.info("Removing reaction from message: {}", messageId);
        return ResponseEntity.ok(ApiResponse.success("Reaction removed", null));
    }

    @PostMapping("/conversations/{conversationId}/mute")
    @Operation(summary = "Mute a conversation")
    public ResponseEntity<ApiResponse<Void>> muteConversation(@PathVariable Long conversationId) {
        log.info("Muting conversation: {}", conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation muted", null));
    }

    @DeleteMapping("/conversations/{conversationId}/mute")
    @Operation(summary = "Unmute a conversation")
    public ResponseEntity<ApiResponse<Void>> unmuteConversation(@PathVariable Long conversationId) {
        log.info("Unmuting conversation: {}", conversationId);
        return ResponseEntity.ok(ApiResponse.success("Conversation unmuted", null));
    }

    @GetMapping("/search")
    @Operation(summary = "Search messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchMessages(@RequestParam String query) {
        log.info("Searching messages: {}", query);
        Page<Message> messages = messageService.searchMessages(query, PageRequest.of(0, 50));
        List<Map<String, Object>> response = messages.getContent().stream()
                .map(this::toMessageMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/conversations/{conversationId}/attachment")
    @Operation(summary = "Send attachment in message")
    public ResponseEntity<ApiResponse<Map<String, Long>>> sendAttachment(
            @PathVariable Long conversationId,
            @RequestParam String attachmentUrl) {
        log.info("Sending attachment to conversation: {}", conversationId);
        Message message = messageService.sendMessage(conversationId, "", attachmentUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Attachment sent", Map.of("messageId", message.getId())));
    }

    private Map<String, Object> toMessageMap(Message message) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", message.getId());
        map.put("senderId", message.getSender().getId());
        map.put("receiverId", message.getReceiver().getId());
        map.put("content", message.getContent());
        map.put("mediaUrl", message.getMediaUrl());
        map.put("timestamp", message.getTimestamp());
        map.put("isRead", message.isRead());
        map.put("isDeleted", message.isDeleted());
        return map;
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("userId", user.getId());
        map.put("username", user.getUsername());
        map.put("name", user.getName());
        map.put("profilePicture", user.getProfilePicture());
        return map;
    }
}
