package org.example.chessserver.service;

import org.example.chessserver.dto.AdminMessageDto;
import org.example.chessserver.dto.AdminMessageRequest;
import org.example.chessserver.entity.AdminMessage;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.AdminMessageRepository;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.websocket.ChessWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMessageServiceTest {

    @Mock
    private AdminMessageRepository adminMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChessWebSocketHandler webSocketHandler;

    @InjectMocks
    private AdminMessageService adminMessageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminMessageService, "webSocketHandler", webSocketHandler);
    }

    @Test
    void testSendMessageSuccess() throws Exception {
        User recipient = new User();
        recipient.setUserId(2);
        recipient.setUsername("testUser");

        when(userRepository.findById(2)).thenReturn(Optional.of(recipient));
        when(adminMessageRepository.save(any(AdminMessage.class))).thenAnswer(invocation -> {
            AdminMessage msg = invocation.getArgument(0);
            msg.setId(100L);
            return msg;
        });
        when(webSocketHandler.isUserOnline(2)).thenReturn(true);

        AdminMessageRequest request = AdminMessageRequest.builder()
                .title("Cảnh báo vi phạm")
                .content("Vui lòng tuân thủ quy tắc")
                .type("WARNING")
                .build();

        AdminMessageDto result = adminMessageService.sendMessage(1, "AdminUser", 2, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals(100L, result.getMessageId());
        assertEquals(2, result.getRecipientId());
        assertEquals("testUser", result.getRecipientUsername());
        assertEquals("AdminUser", result.getSenderUsername());
        assertEquals("Cảnh báo vi phạm", result.getTitle());
        assertEquals("Vui lòng tuân thủ quy tắc", result.getContent());
        assertEquals("WARNING", result.getType());
        assertFalse(result.getIsRead());

        verify(webSocketHandler, times(1)).sendToUser(eq(2), anyString());
    }

    @Test
    void testGetUserMessages() {
        AdminMessage msg = AdminMessage.builder()
                .id(1L)
                .senderId(1)
                .senderUsername("Admin")
                .recipientId(2)
                .recipientUsername("testUser")
                .title("Thông báo")
                .content("Nội dung")
                .type("INFO")
                .isRead(false)
                .sentAt(ZonedDateTime.now())
                .build();

        when(adminMessageRepository.findByRecipientIdOrderBySentAtDesc(2)).thenReturn(List.of(msg));

        List<AdminMessageDto> list = adminMessageService.getUserMessages(2);
        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getId());
        assertEquals("Thông báo", list.get(0).getTitle());
    }

    @Test
    void testMarkAsRead() {
        when(adminMessageRepository.markAsRead(1L, 2)).thenReturn(1);
        boolean updated = adminMessageService.markAsRead(1L, 2);
        assertTrue(updated);
        verify(adminMessageRepository).markAsRead(1L, 2);
    }

    @Test
    void testMarkAllAsRead() {
        adminMessageService.markAllAsRead(2);
        verify(adminMessageRepository).markAllAsRead(2);
    }
}
