package org.example.chessserver.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "preferred_language", length = 10)
    private String preferredLanguage;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "role", length = 20)
    private String role = "ROLE_USER";

    @Column(name = "is_banned")
    private Boolean isBanned = false;

    @Column(name = "ban_reason", length = 255)
    private String banReason;

    @Column(name = "ban_description", length = 1000)
    private String banDescription;

    @Column(name = "banned_by_user", length = 100)
    private String bannedByUser;

    @Column(name = "rainbow_name_enabled")
    private Boolean rainbowNameEnabled = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private ZonedDateTime createdAt;
}
