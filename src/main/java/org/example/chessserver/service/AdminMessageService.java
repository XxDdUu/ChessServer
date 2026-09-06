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
        return adminMessageRepository.findAllByOrderBySentAtDesc()
                .stream()
                .map(AdminMessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean markAsRead(Long messageId, Integer userId) {
        return adminMessageRepository.markAsRead(messageId, userId) > 0;
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        adminMessageRepository.markAllAsRead(userId);
    }
}
