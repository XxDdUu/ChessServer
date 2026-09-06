package org.example.chessserver.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.example.chessserver.dto.UserProfileDto;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.security.JwtUtil;
import org.example.chessserver.service.AdminMessageService;
import org.example.chessserver.service.UserService;
import org.example.chessserver.websocket.ChessWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerRainbowNameTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminMessageService adminMessageService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ChessWebSocketHandler webSocketHandler;

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
        ReflectionTestUtils.setField(userController, "webSocketHandler", webSocketHandler);

        adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setUsername("AdminMaster");
        adminUser.setRole("ROLE_ADMIN");
        adminUser.setRainbowNameEnabled(false);

        regularUser = new User();
        regularUser.setUserId(2);
        regularUser.setUsername("regularPlayer");
        regularUser.setRole("ROLE_USER");
        regularUser.setRainbowNameEnabled(false);
    }

    private void mockAuth(User user) {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(jwtUtil.getClaims("valid_token")).thenReturn(claims);
        when(claims.get("userId", Integer.class)).thenReturn(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
    }

    @Test
    void testUpdateRainbowName_SuccessForAdmin_EnablesRainbow() {
        mockAuth(adminUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of("rainbowNameEnabled", true);
        ResponseEntity<?> response = userController.updateRainbowName(body, request);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(adminUser.getRainbowNameEnabled());
        verify(userRepository, times(1)).save(adminUser);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketHandler, times(1)).broadcastToAllOnline(msgCaptor.capture());
        String wsMessage = msgCaptor.getValue();
        assertTrue(wsMessage.contains("\"type\":\"ADMIN_PROFILE_UPDATED\""));
        assertTrue(wsMessage.contains("\"rainbowNameEnabled\":true"));
        assertTrue(wsMessage.contains("\"adminId\":1"));
    }

    @Test
    void testUpdateRainbowName_SuccessForAdmin_DisablesRainbow() {
        adminUser.setRainbowNameEnabled(true);
        mockAuth(adminUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of("rainbowNameEnabled", false);
        ResponseEntity<?> response = userController.updateRainbowName(body, request);

        assertEquals(200, response.getStatusCode().value());
        assertFalse(adminUser.getRainbowNameEnabled());
        verify(userRepository, times(1)).save(adminUser);

        ArgumentCaptor<String> msgCaptor = ArgumentCaptor.forClass(String.class);
        verify(webSocketHandler, times(1)).broadcastToAllOnline(msgCaptor.capture());
        String wsMessage = msgCaptor.getValue();
        assertTrue(wsMessage.contains("\"rainbowNameEnabled\":false"));
    }

    @Test
    void testUpdateRainbowName_ForbiddenForRegularUser() {
        mockAuth(regularUser);

        Map<String, Object> body = Map.of("rainbowNameEnabled", true);
        assertThrows(AccessDeniedException.class, () -> {
            userController.updateRainbowName(body, request);
        });

        verify(userRepository, never()).save(any());
        verify(webSocketHandler, never()).broadcastToAllOnline(any());
    }

    @Test
    void testGetAdminRainbowStatuses() {
        adminUser.setRainbowNameEnabled(true);
        when(userRepository.findAllAdmins()).thenReturn(List.of(adminUser));

        ResponseEntity<List<Map<String, Object>>> response = userController.getAdminRainbowStatuses();
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        Map<String, Object> adminData = response.getBody().get(0);
        assertEquals(1, adminData.get("adminId"));
        assertEquals("AdminMaster", adminData.get("adminUsername"));
        assertEquals(true, adminData.get("rainbowNameEnabled"));
    }

    @Test
    void testGetMe_ReturnsProfileWithRainbowEnabled() {
        mockAuth(adminUser);
        UserProfileDto dto = UserProfileDto.builder()
                .userId(1)
                .username("AdminMaster")
                .role("ROLE_ADMIN")
                .rainbowNameEnabled(true)
                .build();
        when(userService.getUserProfile(1, 1)).thenReturn(dto);

        ResponseEntity<UserProfileDto> response = userController.getMe(request);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getRainbowNameEnabled());
        assertEquals("ROLE_ADMIN", response.getBody().getRole());
    }
}
