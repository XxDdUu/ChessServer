package org.example.chessserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.chessserver.dto.AdminMessageDto;
import org.example.chessserver.dto.AdminMessageRequest;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.security.JwtUtil;
import org.example.chessserver.service.AdminMessageService;
import org.example.chessserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import io.jsonwebtoken.Claims;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerMessagesTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminMessageService adminMessageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Claims claims;

    @InjectMocks
    private UserController userController;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setUsername("Admin");
        adminUser.setRole("ROLE_ADMIN");

        regularUser = new User();
        regularUser.setUserId(2);
        regularUser.setUsername("player1");
        regularUser.setRole("ROLE_USER");
    }

    private void mockAuth(User user) {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(jwtUtil.getClaims("valid_token")).thenReturn(claims);
        when(claims.get("userId", Integer.class)).thenReturn(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
    }

    @Test
    void testGetUserMessages_AsOwner() {
        mockAuth(regularUser);
        when(adminMessageService.getUserMessages(2)).thenReturn(List.of(
                AdminMessageDto.builder().id(1L).title("Test").build()
        ));

        ResponseEntity<List<AdminMessageDto>> response = userController.getUserMessages(2, request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("Test", response.getBody().get(0).getTitle());
    }

    @Test
    void testGetUserMessages_ForbiddenForDifferentUser() {
        mockAuth(regularUser);

        assertThrows(AccessDeniedException.class, () -> {
            userController.getUserMessages(3, request);
        });
    }

    @Test
    void testGetUserMessages_AllowedForAdmin() {
        mockAuth(adminUser);
        when(adminMessageService.getUserMessages(3)).thenReturn(List.of());

        ResponseEntity<List<AdminMessageDto>> response = userController.getUserMessages(3, request);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testSendMessageToUser_AllowedForAdmin() {
        mockAuth(adminUser);
        AdminMessageRequest req = AdminMessageRequest.builder()
                .title("Notice")
                .content("Content")
                .build();

        AdminMessageDto createdDto = AdminMessageDto.builder().id(10L).title("Notice").build();
        when(adminMessageService.sendMessage(eq(1), eq("Admin"), eq(2), any(AdminMessageRequest.class)))
                .thenReturn(createdDto);

        ResponseEntity<AdminMessageDto> response = userController.sendMessageToUser(2, req, request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(10L, response.getBody().getId());
    }

    @Test
    void testSendMessageToUser_ForbiddenForRegularUser() {
        mockAuth(regularUser);
        AdminMessageRequest req = AdminMessageRequest.builder()
                .title("Notice")
                .content("Content")
                .build();

        assertThrows(AccessDeniedException.class, () -> {
            userController.sendMessageToUser(3, req, request);
        });
    }

    @Test
    void testMarkMessageAsRead() {
        mockAuth(regularUser);
        when(adminMessageService.markAsRead(5L, 2)).thenReturn(true);

        ResponseEntity<?> response = userController.markMessageAsRead(2, 5L, request);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody() instanceof Map);
        assertEquals(true, ((Map<?, ?>) response.getBody()).get("success"));
    }
}
