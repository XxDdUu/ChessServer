package org.example.chessserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "admin_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @Column(name = "sender_id")
    private Integer senderId;

    @Column(name = "sender_username", length = 50)
    private String senderUsername;

    @Column(name = "recipient_id", nullable = false)
    private Integer recipientId;

    @Column(name = "recipient_username", length = 50)
    private String recipientUsername;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "type", length = 30)
    private String type;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "sent_at")
    private ZonedDateTime sentAt;
}
