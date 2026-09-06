package org.example.chessserver.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMessageRequest {
    @JsonAlias({"subject", "topic"})
    private String title;

    @JsonAlias({"message", "body", "text"})
    private String content;

    @JsonAlias({"category", "messageType"})
    private String type;

    @JsonAlias({"receiverId", "toUserId", "targetUserId"})
    private Integer recipientId;

    @JsonAlias({"receiverUsername", "toUsername", "targetUsername"})
    private String recipientUsername;

    @JsonAlias({"senderUsername"})
    private String senderName;

    @JsonAlias({"broadcast", "isBroadcast", "allUsers", "toAll"})
    private Boolean sendToAll;
}
