package org.example.chessserver.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.security.JwtUtil;
import org.example.chessserver.service.AdminMessageService;
import org.example.chessserver.service.UserService;
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

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerUserPreferencesTest {

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

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(10);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole("ROLE_USER");
        testUser.setPreferredLanguage(null);
    }

    private void mockAuth(User user) {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer valid_token");
        when(jwtUtil.getClaims("valid_token")).thenReturn(claims);
        when(claims.get("userId", Integer.class)).thenReturn(user.getUserId());
        when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
    }

    @Test
    void testUpdatePreferences_Success_Vietnamese() {
        mockAuth(testUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of("language", "vi");
        ResponseEntity<?> response = userController.updatePreferences(body, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("vi", testUser.getPreferredLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdatePreferences_Success_Chinese() {
        mockAuth(testUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of("language", "zh");
        ResponseEntity<?> response = userController.updatePreferences(body, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("zh", testUser.getPreferredLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdatePreferences_Success_Korean() {
        mockAuth(testUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of("language", "ko");
        ResponseEntity<?> response = userController.updatePreferences(body, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ko", testUser.getPreferredLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdatePreferences_Success_English() {
        mockAuth(testUser);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> body = Map.of("language", "en");
        ResponseEntity<?> response = userController.updatePreferences(body, request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("en", testUser.getPreferredLanguage());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testUpdatePreferences_BadRequest_UnsupportedLanguage() {
        mockAuth(testUser);

        Map<String, Object> body = Map.of("language", "fr");
        ResponseEntity<?> response = userController.updatePreferences(body, request);

        assertEquals(400, response.getStatusCode().value());
        assertNull(testUser.getPreferredLanguage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdatePreferences_BadRequest_NullOrMissingLanguage() {
        mockAuth(testUser);

        Map<String, Object> body = Map.of();
        ResponseEntity<?> response = userController.updatePreferences(body, request);

        assertEquals(400, response.getStatusCode().value());
        assertNull(testUser.getPreferredLanguage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testUpdatePreferences_Unauthenticated_ThrowsException() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        Map<String, Object> body = Map.of("language", "en");
        assertThrows(AccessDeniedException.class, () -> {
            userController.updatePreferences(body, request);
        });

        verify(userRepository, never()).save(any());
    }
}
