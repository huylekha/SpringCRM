package com.company.platform.auth.auth.service;

import com.company.platform.auth.config.JwtProperties;
import com.company.platform.shared.security.SecurityConstants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        String secret = props.getSecret();
        if (secret == null || secret.length() < 64) {
            secret = "default-jwt-secret-key-for-development-must-be-at-least-64-chars-long!!";
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String userId, List<String> roles, List<String> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(props.getIssuer())
                .subject(userId)
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_CLAIMS, claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(props.getAccessTokenExpiry())))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshTokenValue() {
        return UUID.randomUUID().toString();
    }

    public int getAccessTokenExpiry() {
        return props.getAccessTokenExpiry();
    }

    public int getRefreshTokenExpiry() {
        return props.getRefreshTokenExpiry();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return parseAccessToken(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseAccessToken(token).get(SecurityConstants.CLAIM_ROLES, List.class);
    }
}
