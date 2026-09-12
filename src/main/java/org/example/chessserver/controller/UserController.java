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
import org.example.chessserver.websocket.ChessWebSocketHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/user", "/api/users"})
@RequiredArgsConstructor
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AdminMessageService adminMessageService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Autowired(required = false)
    @Lazy
    private ChessWebSocketHandler webSocketHandler;

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

    @PostMapping("/messages/broadcast")
    public ResponseEntity<?> broadcastMessage(
            @RequestBody AdminMessageRequest messageRequest,
            HttpServletRequest request) {
        User caller = extractUser(request);
        if (!"ROLE_ADMIN".equals(caller.getRole())) {
            throw new AccessDeniedException("Admin access required to broadcast messages");
        }
        Map<String, Object> result = adminMessageService.broadcastMessage(caller.getUserId(), caller.getUsername(), messageRequest);
        return ResponseEntity.ok(result);
    }

    private boolean isAdmin(User user) {
        if (user == null || user.getRole() == null) return false;
        String role = user.getRole().trim();
        return "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(user.getUsername());
    }

    @RequestMapping(value = "/me/rainbow-name", method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> updateRainbowName(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return updateProfile(body, request);
    }

    @RequestMapping(value = {"/me/profile", "/me"}, method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> updateProfile(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        User caller = extractUser(request);

        Boolean rainbowEnabled = caller.getRainbowNameEnabled();

        if (body != null) {
            if (body.containsKey("avatarUrl")) {
                Object val = body.get("avatarUrl");
                if (val != null) caller.setAvatarUrl(val.toString());
            } else if (body.containsKey("avatar")) {
                Object val = body.get("avatar");
                if (val != null) caller.setAvatarUrl(val.toString());
            }

            if (body.containsKey("bio")) {
                Object val = body.get("bio");
                if (val != null) caller.setBio(val.toString());
            }

            if (body.containsKey("countryCode")) {
                Object val = body.get("countryCode");
                if (val != null) caller.setCountryCode(val.toString());
            }

            if (body.containsKey("rainbowNameEnabled") || body.containsKey("enabled")) {
                if (!isAdmin(caller)) {
                    throw new AccessDeniedException("Only administrators can configure rainbow admin name");
                }
                Object val = body.containsKey("rainbowNameEnabled") ? body.get("rainbowNameEnabled") : body.get("enabled");
                if (val instanceof Boolean) {
                    rainbowEnabled = (Boolean) val;
                } else if (val != null) {
                    rainbowEnabled = Boolean.parseBoolean(val.toString());
                }
                caller.setRainbowNameEnabled(rainbowEnabled);
            }
        }

        userRepository.save(caller);

        // Broadcast real-time WebSocket event if rainbow status changed
        if (body != null && (body.containsKey("rainbowNameEnabled") || body.containsKey("enabled"))) {
            try {
                if (webSocketHandler != null) {
                    JSONObject wsMsg = new JSONObject()
                            .put("type", "ADMIN_PROFILE_UPDATED")
                            .put("adminId", caller.getUserId())
                            .put("userId", caller.getUserId())
                            .put("adminUsername", caller.getUsername())
                            .put("rainbowNameEnabled", Boolean.TRUE.equals(caller.getRainbowNameEnabled()));
                    webSocketHandler.broadcastToAllOnline(wsMsg.toString());
                }
            } catch (Exception e) {
                log.warn("Failed to broadcast real-time rainbow status update: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", caller.getUserId(),
                "username", caller.getUsername(),
                "avatarUrl", caller.getAvatarUrl() != null ? caller.getAvatarUrl() : "",
                "bio", caller.getBio() != null ? caller.getBio() : "",
                "countryCode", caller.getCountryCode() != null ? caller.getCountryCode() : "",
                "rainbowNameEnabled", Boolean.TRUE.equals(caller.getRainbowNameEnabled())
        ));
    }

    @GetMapping("/admins/rainbow-status")
    public ResponseEntity<List<Map<String, Object>>> getAdminRainbowStatuses() {
        List<User> admins = userRepository.findAllAdmins();
        List<Map<String, Object>> result = admins.stream()
                .map(admin -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("adminId", admin.getUserId());
                    map.put("userId", admin.getUserId());
                    map.put("adminUsername", admin.getUsername());
                    map.put("username", admin.getUsername());
                    map.put("role", admin.getRole());
                    map.put("rainbowNameEnabled", Boolean.TRUE.equals(admin.getRainbowNameEnabled()));
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @RequestMapping(value = {"/me/preferences", "/me/language"}, method = {RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> updatePreferences(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        User caller = extractUser(request);

        String lang = null;
        if (body != null) {
            if (body.containsKey("language")) {
                Object val = body.get("language");
                if (val != null) lang = val.toString().trim().toLowerCase();
            } else if (body.containsKey("preferredLanguage")) {
                Object val = body.get("preferredLanguage");
                if (val != null) lang = val.toString().trim().toLowerCase();
            }
        }

        List<String> supportedLanguages = List.of("vi", "en", "zh", "ko");
        if (lang == null || !supportedLanguages.contains(lang)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", "Bad Request",
                    "message", "Unsupported language value. Allowed values are: vi, en, zh, ko"
            ));
        }

        caller.setPreferredLanguage(lang);
        userRepository.save(caller);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "userId", caller.getUserId(),
                "preferredLanguage", lang
        ));
    }
}
