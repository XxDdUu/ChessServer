package org.example.chessserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.chessserver.dto.AdminMessageDto;
import org.example.chessserver.dto.AdminMessageRequest;
import org.example.chessserver.dto.LeaderboardDto;
import org.example.chessserver.dto.UserProfileDto;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.security.JwtUtil;
import org.example.chessserver.service.AdminMessageService;
import org.example.chessserver.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final AdminMessageService adminMessageService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    private User extractUser(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new AccessDeniedException("Authorization header is missing or invalid");
        }
        String token = authHeader.substring(7);
        int userId = jwtUtil.getClaims(token).get("userId", Integer.class);
        return userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMe(HttpServletRequest request) {
        User user = extractUser(request);
        return ResponseEntity.ok(userService.getUserProfile(user.getUserId(), user.getUserId()));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<UserProfileDto> getUserStats(
            @PathVariable int id,
            HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        Integer currentUserId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            currentUserId = jwtUtil.getClaims(token).get("userId", Integer.class);
        }
        return ResponseEntity.ok(userService.getUserProfile(id, currentUserId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDto>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    @GetMapping("/{userId}/messages")
    public ResponseEntity<List<AdminMessageDto>> getUserMessages(
            @PathVariable int userId,
            HttpServletRequest request) {
        User caller = extractUser(request);
        if (caller.getUserId() != userId && !"ROLE_ADMIN".equals(caller.getRole())) {
            throw new AccessDeniedException("Access denied to another user's messages");
        }
        return ResponseEntity.ok(adminMessageService.getUserMessages(userId));
    }

    @PostMapping("/{userId}/messages")
    public ResponseEntity<AdminMessageDto> sendMessageToUser(
            @PathVariable int userId,
            @RequestBody AdminMessageRequest messageRequest,
            HttpServletRequest request) {
        User caller = extractUser(request);
        if (!"ROLE_ADMIN".equals(caller.getRole())) {
            throw new AccessDeniedException("Admin access required to send admin messages");
        }
        AdminMessageDto created = adminMessageService.sendMessage(caller.getUserId(), caller.getUsername(), userId, messageRequest);
        return ResponseEntity.ok(created);
    }

    @RequestMapping(value = "/{userId}/messages/{messageId}/read", method = {RequestMethod.POST, RequestMethod.PATCH})
    public ResponseEntity<?> markMessageAsRead(
            @PathVariable int userId,
            @PathVariable Long messageId,
            HttpServletRequest request) {
        User caller = extractUser(request);
        if (caller.getUserId() != userId && !"ROLE_ADMIN".equals(caller.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        boolean updated = adminMessageService.markAsRead(messageId, userId);
        return ResponseEntity.ok(Map.of("success", updated, "messageId", messageId));
    }

    @PostMapping("/{userId}/messages/read-all")
    public ResponseEntity<?> markAllMessagesAsRead(
            @PathVariable int userId,
            HttpServletRequest request) {
        User caller = extractUser(request);
        if (caller.getUserId() != userId && !"ROLE_ADMIN".equals(caller.getRole())) {
            throw new AccessDeniedException("Access denied");
        }
        adminMessageService.markAllAsRead(userId);
        return ResponseEntity.ok(Map.of("success", true, "userId", userId));
    }
}
