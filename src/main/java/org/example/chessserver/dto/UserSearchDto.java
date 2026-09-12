package org.example.chessserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchDto {
    private int userId;
    private String username;
    private int rating;
    private String friendshipStatus;
    private String avatarUrl;
    private String bio;
    private String countryCode;
    private String role;
    private Boolean rainbowNameEnabled;
}
