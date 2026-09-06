package org.example.chessserver.service;

import lombok.RequiredArgsConstructor;
import org.example.chessserver.dto.AdminMessageDto;
import org.example.chessserver.dto.AdminMessageRequest;
import org.example.chessserver.entity.AdminMessage;
import org.example.chessserver.entity.User;
import org.example.chessserver.repository.AdminMessageRepository;
import org.example.chessserver.repository.UserRepository;
import org.example.chessserver.websocket.ChessWebSocketHandler;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMessageService {
    private static final Logger log = LoggerFactory.getLogger(AdminMessageService.class);

    private final AdminMessageRepository adminMessageRepository;
    private final UserRepository userRepository;

    @Autowired
    @Lazy
    private ChessWebSocketHandler webSocketHandler;

    @Transactional
    public AdminMessageDto sendMessage(Integer senderId, String senderUsername, Integer recipientId, AdminMessageRequest request) {
        if (recipientId == null) {
            throw new IllegalArgumentException("Recipient ID cannot be null");
        }

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found with id: " + recipientId));

        String resolvedSender = senderUsername;
        if (resolvedSender == null || resolvedSender.isBlank()) {
            if (senderId != null) {
                resolvedSender = userRepository.findById(senderId).map(User::getUsername).orElse("Admin");
            } else {
                resolvedSender = "Admin";
            }
        }

        String resolvedRecipientName = (request.getRecipientUsername() != null && !request.getRecipientUsername().isBlank())
                ? request.getRecipientUsername()
                : recipient.getUsername();

        String resolvedTitle = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : "Thông báo từ Quản trị viên";

        String resolvedContent = request.getContent() != null ? request.getContent() : "";

        String resolvedType = (request.getType() != null && !request.getType().isBlank())
                ? request.getType().toUpperCase()
                : "INFO";

        AdminMessage entity = AdminMessage.builder()
                .senderId(senderId)
                .senderUsername(resolvedSender)
                .recipientId(recipientId)
                .recipientUsername(resolvedRecipientName)
                .title(resolvedTitle)
                .content(resolvedContent)
                .type(resolvedType)
                .isRead(false)
                .sentAt(ZonedDateTime.now())
                .build();

        AdminMessage saved = adminMessageRepository.save(entity);
        AdminMessageDto dto = AdminMessageDto.fromEntity(saved);

        // Broadcast to recipient via WebSocket in real time
        try {
            if (webSocketHandler != null && webSocketHandler.isUserOnline(recipientId)) {
                JSONObject wsMsg = new JSONObject()
                        .put("type", "ADMIN_DIRECT_MESSAGE")
                        .put("id", saved.getId())
                        .put("messageId", saved.getId())
                        .put("recipientId", recipientId)
                        .put("receiverId", recipientId)
                        .put("recipientUsername", resolvedRecipientName)
                        .put("receiverUsername", resolvedRecipientName)
                        .put("senderUsername", resolvedSender)
                        .put("senderName", resolvedSender)
                        .put("senderId", senderId)
                        .put("title", resolvedTitle)
                        .put("subject", resolvedTitle)
                        .put("content", resolvedContent)
                        .put("message", resolvedContent)
                        .put("body", resolvedContent)
                        .put("messageType", resolvedType)
                        .put("category", resolvedType)
                        .put("sentAt", saved.getSentAt().toString())
                        .put("createdAt", saved.getSentAt().toString())
                        .put("read", false)
                        .put("isRead", false);

                webSocketHandler.sendToUser(recipientId, wsMsg.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to deliver real-time WebSocket message to user {}: {}", recipientId, e.getMessage());
        }

        return dto;
    }

    public List<AdminMessageDto> getUserMessages(Integer recipientId) {
        return adminMessageRepository.findByRecipientIdOrderBySentAtDesc(recipientId)
                .stream()
                .map(AdminMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AdminMessageDto> getAllMessages() {
        List<AdminMessage> all = adminMessageRepository.findAllByOrderBySentAtDesc();
        java.util.Map<String, AdminMessageDto> broadcastSeen = new java.util.LinkedHashMap<>();
        List<AdminMessageDto> result = new java.util.ArrayList<>();

        for (AdminMessage m : all) {
            if (Boolean.TRUE.equals(m.getIsBroadcast())) {
                String key = (m.getTitle() != null ? m.getTitle() : "") + "|" +
                             (m.getContent() != null ? m.getContent() : "") + "|" +
                             (m.getSentAt() != null ? m.getSentAt().toEpochSecond() : "");
                if (!broadcastSeen.containsKey(key)) {
                    AdminMessageDto dto = AdminMessageDto.fromEntity(m);
                    dto.setRecipientUsername("Tất cả người dùng");
                    dto.setReceiverUsername("Tất cả người dùng");
                    dto.setRecipientId(null);
                    dto.setReceiverId(null);
                    dto.setIsBroadcast(true);
                    dto.setSendToAll(true);
                    broadcastSeen.put(key, dto);
                    result.add(dto);
                }
            } else {
                result.add(AdminMessageDto.fromEntity(m));
            }
        }
        return result;
    }

    @Transactional
    public boolean markAsRead(Long messageId, Integer userId) {
        return adminMessageRepository.markAsRead(messageId, userId) > 0;
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        adminMessageRepository.markAllAsRead(userId);
    }

    @Transactional
    public java.util.Map<String, Object> broadcastMessage(Integer senderId, String senderUsername, AdminMessageRequest request) {
        String tempSender = senderUsername;
        if (tempSender == null || tempSender.isBlank()) {
            if (senderId != null) {
                tempSender = userRepository.findById(senderId).map(User::getUsername).orElse("Admin");
            } else {
                tempSender = "Admin";
            }
        }
        final String resolvedSender = tempSender;

        String resolvedTitle = (request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle()
                : "Thông báo từ Quản trị viên";

        String resolvedContent = request.getContent() != null ? request.getContent() : "";

        String resolvedType = (request.getType() != null && !request.getType().isBlank())
                ? request.getType().toUpperCase()
                : "ANNOUNCEMENT";

        List<User> allUsers = userRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now();

        List<AdminMessage> messages = allUsers.stream().map(u -> AdminMessage.builder()
                .senderId(senderId)
                .senderUsername(resolvedSender)
                .recipientId(u.getUserId())
                .recipientUsername(u.getUsername())
                .title(resolvedTitle)
                .content(resolvedContent)
                .type(resolvedType)
                .isRead(false)
                .isBroadcast(true)
                .sentAt(now)
                .build()
        ).collect(Collectors.toList());

        List<AdminMessage> savedMessages = adminMessageRepository.saveAll(messages);

        // Real-time broadcast to all currently connected users
        int onlineNotified = 0;
        try {
            if (webSocketHandler != null) {
                java.util.Set<Integer> onlineIds = webSocketHandler.getOnlineUserIds();
                for (AdminMessage m : savedMessages) {
                    if (onlineIds.contains(m.getRecipientId())) {
                        JSONObject wsMsg = new JSONObject()
                                .put("type", "ADMIN_DIRECT_MESSAGE")
                                .put("id", m.getId())
                                .put("messageId", m.getId())
                                .put("recipientId", m.getRecipientId())
                                .put("receiverId", m.getRecipientId())
                                .put("recipientUsername", m.getRecipientUsername())
                                .put("receiverUsername", m.getRecipientUsername())
                                .put("senderUsername", resolvedSender)
                                .put("senderName", resolvedSender)
                                .put("senderId", senderId)
                                .put("title", resolvedTitle)
                                .put("subject", resolvedTitle)
                                .put("content", resolvedContent)
                                .put("message", resolvedContent)
                                .put("body", resolvedContent)
                                .put("messageType", resolvedType)
                                .put("category", resolvedType)
                                .put("sentAt", m.getSentAt().toString())
                                .put("createdAt", m.getSentAt().toString())
                                .put("read", false)
                                .put("isRead", false)
                                .put("isBroadcast", true);

                        webSocketHandler.sendToUser(m.getRecipientId(), wsMsg.toString());
                        onlineNotified++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to broadcast real-time WebSocket message: {}", e.getMessage());
        }

        return java.util.Map.of(
                "message", "Broadcast message sent successfully to all users",
                "totalRecipients", savedMessages.size(),
                "onlineRecipientsNotified", onlineNotified,
                "title", resolvedTitle,
                "type", resolvedType,
                "sentAt", now.toString()
        );
    }
}
