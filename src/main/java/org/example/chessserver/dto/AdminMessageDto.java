package org.example.chessserver.dto;

import lombok.*;
import org.example.chessserver.entity.AdminMessage;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMessageDto {
    private Long id;
    private Long messageId;
    private Integer senderId;
    private String senderUsername;
    private String senderName;
    private Integer recipientId;
    private Integer receiverId;
    private String recipientUsername;
    private String receiverUsername;
    private String title;
    private String subject;
    private String content;
    private String message;
    private String body;
    private String type;
    private Boolean read;
    private Boolean isRead;
    private ZonedDateTime sentAt;
    private ZonedDateTime createdAt;

    public static AdminMessageDto fromEntity(AdminMessage entity) {
        if (entity == null) return null;
        boolean readStatus = Boolean.TRUE.equals(entity.getIsRead());
        return AdminMessageDto.builder()
                .id(entity.getId())
                .messageId(entity.getId())
                .senderId(entity.getSenderId())
                .senderUsername(entity.getSenderUsername())
                .senderName(entity.getSenderUsername())
                .recipientId(entity.getRecipientId())
                .receiverId(entity.getRecipientId())
                .recipientUsername(entity.getRecipientUsername())
                .receiverUsername(entity.getRecipientUsername())
                .title(entity.getTitle())
                .subject(entity.getTitle())
                .content(entity.getContent())
                .message(entity.getContent())
                .body(entity.getContent())
                .type(entity.getType() != null ? entity.getType() : "INFO")
                .read(readStatus)
                .isRead(readStatus)
                .sentAt(entity.getSentAt())
                .createdAt(entity.getSentAt())
                .build();
    }
}
