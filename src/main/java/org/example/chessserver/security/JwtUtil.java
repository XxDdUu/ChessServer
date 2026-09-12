package org.example.chessserver.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email, Integer userId, String role, String username, Boolean isBanned) {
        String cleanUsername = username;
        if (cleanUsername != null && cleanUsername.contains("@")) {
            cleanUsername = cleanUsername.substring(0, cleanUsername.indexOf("@"));
        }
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .claim("username", cleanUsername)
                .claim("isBanned", Boolean.TRUE.equals(isBanned))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public String generateToken(String email, Integer userId, String role, String username) {
        return generateToken(email, userId, role, username, false);
    }

    public String generateToken(String email, Integer userId, String role) {
        return generateToken(email, userId, role, null, false);
    }

    public String generateToken(String email, Integer userId) {
        return generateToken(email, userId, "ROLE_USER", null, false);
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }   
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }
}
