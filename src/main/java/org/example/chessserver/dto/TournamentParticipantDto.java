package org.example.chessserver.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentParticipantDto {
    private Integer userId;
    private String username;
    private String countryCode;
    private String avatarUrl;
    private String role;
    private Boolean rainbowNameEnabled;
    private Integer initialRating;
    private BigDecimal currentScore;
    private BigDecimal buchholz;
    private BigDecimal sonnebornBerger;
    private Boolean byeReceived;
}
