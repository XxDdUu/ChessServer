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

    @Test
    void testBroadcastMessage() throws Exception {
        User u1 = new User();
        u1.setUserId(2);
        u1.setUsername("user1");

        User u2 = new User();
        u2.setUserId(3);
        u2.setUsername("user2");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(adminMessageRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<AdminMessage> list = inv.getArgument(0);
            long id = 10;
            for (AdminMessage m : list) {
                m.setId(id++);
            }
            return list;
        });

        when(webSocketHandler.getOnlineUserIds()).thenReturn(java.util.Set.of(2));

        AdminMessageRequest req = AdminMessageRequest.builder()
                .title("Thông báo bảo trì")
                .content("Hệ thống bảo trì lúc 23:00")
                .type("ANNOUNCEMENT")
                .build();

        java.util.Map<String, Object> res = adminMessageService.broadcastMessage(1, "Admin", req);

        assertNotNull(res);
        assertEquals(2, res.get("totalRecipients"));
        assertEquals(1, res.get("onlineRecipientsNotified"));
        verify(adminMessageRepository, times(1)).saveAll(anyList());
        verify(webSocketHandler, times(1)).sendToUser(eq(2), anyString());
        verify(webSocketHandler, never()).sendToUser(eq(3), anyString());
    }
}
