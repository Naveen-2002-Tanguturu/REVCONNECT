package org.revature.revconnect.service;

import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.MessageRepository;
import org.revature.revconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private User receiver;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .id(1L)
                .username("sender")
                .email("sender@example.com")
                .name("Sender User")
                .build();

        receiver = User.builder()
                .id(2L)
                .username("receiver")
                .email("receiver@example.com")
                .name("Receiver User")
                .build();

        testMessage = Message.builder()
                .id(1L)
                .sender(sender)
                .receiver(receiver)
                .content("Hello!")
                .mediaUrl(null)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .isDeleted(false)
                .build();
    }

    @Test
    void sendMessage_Success() {
        when(authService.getCurrentUser()).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        Message result = messageService.sendMessage(2L, "Hello!", null);

        assertNotNull(result);
        assertEquals("Hello!", result.getContent());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_ReceiverNotFound() {
        when(authService.getCurrentUser()).thenReturn(sender);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> messageService.sendMessage(99L, "Hello!", null));
    }

    @Test
    void getConversation_Success() {
        Page<Message> page = new PageImpl<>(List.of(testMessage));
        when(authService.getCurrentUser()).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.findConversation(eq(sender), eq(receiver), any()))
                .thenReturn(page);

        Page<Message> result = messageService.getConversation(2L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getConversationPartners_Success() {
        when(authService.getCurrentUser()).thenReturn(sender);
        when(messageRepository.findReceiversForUser(sender)).thenReturn(List.of(receiver));
        when(messageRepository.findSendersForUser(sender)).thenReturn(List.of());

        List<User> partners = messageService.getConversationPartners();

        assertNotNull(partners);
        assertEquals(1, partners.size());
        assertEquals("receiver", partners.get(0).getUsername());
    }

    @Test
    void markAsRead_Success() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(authService.getCurrentUser()).thenReturn(receiver);

        messageService.markAsRead(1L);

        assertTrue(testMessage.isRead());
        verify(messageRepository).save(testMessage);
    }

    @Test
    void markAsRead_NotReceiver_NoUpdate() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(authService.getCurrentUser()).thenReturn(sender);

        messageService.markAsRead(1L);

        assertFalse(testMessage.isRead());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void markAsRead_MessageNotFound() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.markAsRead(99L));
    }

    @Test
    void markAllAsRead_Success() {
        Message unreadMsg = Message.builder()
                .id(2L)
                .sender(receiver)
                .receiver(sender)
                .content("Hey!")
                .isRead(false)
                .build();

        when(authService.getCurrentUser()).thenReturn(sender);
        when(userRepository.findById(2L)).thenReturn(Optional.of(receiver));
        when(messageRepository.findByReceiverAndIsReadFalse(sender)).thenReturn(List.of(unreadMsg));

        messageService.markAllAsRead(2L);

        assertTrue(unreadMsg.isRead());
        verify(messageRepository).saveAll(any());
    }

    @Test
    void getUnreadCount_Success() {
        when(authService.getCurrentUser()).thenReturn(receiver);
        when(messageRepository.countByReceiverAndIsReadFalse(receiver)).thenReturn(5L);

        long count = messageService.getUnreadCount();

        assertEquals(5L, count);
    }

    @Test
    void deleteMessage_Success() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(authService.getCurrentUser()).thenReturn(sender);

        messageService.deleteMessage(1L);

        assertTrue(testMessage.isDeleted());
        verify(messageRepository).save(testMessage);
    }

    @Test
    void deleteMessage_NotSender_NoUpdate() {
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(authService.getCurrentUser()).thenReturn(receiver);

        messageService.deleteMessage(1L);

        assertFalse(testMessage.isDeleted());
        verify(messageRepository, never()).save(any());
    }
}
