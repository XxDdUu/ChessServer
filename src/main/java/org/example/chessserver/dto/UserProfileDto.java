package org.example.chessserver.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserProfileDto {
    private Integer userId;
    private String username;
    private String countryCode;
    private String preferredLanguage;
    private String avatarUrl;
    private String bio;
    private Integer rating;
    private Integer gamesPlayed;
    private Integer wins;
    private Integer losses;
    private Integer draws;
    private Double winRate;
    private Integer goldMedals;
    private Integer silverMedals;
    private Integer bronzeMedals;
    private String friendshipStatus;
    private String role;
    private Boolean isBanned;
    private String banReason;
    private String banDescription;
    private String bannedByUser;
    private Boolean rainbowNameEnabled;
    private List<UserTournamentDto> tournamentHistory;
}
