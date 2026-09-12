package org.example.chessserver.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentPairingDto {
    private Integer pairingId;
    private Integer roundId;
    private Integer whitePlayerId;
    private String whitePlayerName;
    private Integer whitePlayerRating;
    private String whitePlayerAvatar;
    private Integer blackPlayerId;
    private String blackPlayerName;
    private Integer blackPlayerRating;
    private String blackPlayerAvatar;
    private Integer gameId;
    private String result;
    private Boolean isBye;
}
