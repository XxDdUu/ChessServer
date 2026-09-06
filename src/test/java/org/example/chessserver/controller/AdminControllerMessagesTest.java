package org.example.chessserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.chessserver.dto.AdminMessageDto;
import org.example.chessserver.dto.AdminMessageRequest;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.security.JwtUtil;
import org.example.chessserver.service.AdminMessageService;
import org.example.chessserver.service.AdminService;
import org.example.chessserver.service.TournamentService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerMessagesTest {

    @Mock
    private AdminService adminService;

    @Mock
    private AdminMessageService adminMessageService;

    @Mock
    private TournamentService tournamentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private Claims claims;

    @InjectMocks
    private AdminController adminController;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUserId(1);
        adminUser.setUsername("AdminMaster");
        adminUser.setRole("ROLE_ADMIN");
    }

    private void mockAdminAuth() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer admin_token");
        when(jwtUtil.getClaims("admin_token")).thenReturn(claims);
        when(claims.get("userId", Integer.class)).thenReturn(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(adminUser));
    }

    @Test
    void testGetUserDirectMessages() {
        mockAdminAuth();
        when(adminMessageService.getUserMessages(5)).thenReturn(List.of(
                AdminMessageDto.builder().id(20L).recipientId(5).title("Hello").build()
        ));

        ResponseEntity<List<AdminMessageDto>> response = adminController.getUserDirectMessages(request, 5);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(20L, response.getBody().get(0).getId());
    }

    @Test
    void testSendDirectMessageToUser() {
        mockAdminAuth();
        AdminMessageRequest req = AdminMessageRequest.builder()
                .title("Cảnh báo")
                .content("Vi phạm")
                .build();

        AdminMessageDto dto = AdminMessageDto.builder().id(21L).recipientId(5).title("Cảnh báo").build();
        when(adminMessageService.sendMessage(eq(1), eq("AdminMaster"), eq(5), any(AdminMessageRequest.class)))
                .thenReturn(dto);

        ResponseEntity<AdminMessageDto> response = adminController.sendDirectMessageToUser(request, 5, req);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(21L, response.getBody().getId());
    }

    @Test
    void testGetAllDirectMessages() {
        mockAdminAuth();
        when(adminMessageService.getAllMessages()).thenReturn(List.of(
                AdminMessageDto.builder().id(1L).build(),
                AdminMessageDto.builder().id(2L).build()
        ));

        ResponseEntity<List<AdminMessageDto>> response = adminController.getAllDirectMessages(request);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(2, response.getBody().size());
    }
}
