package com.campus.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    @Value("${campus.jwt.access-secret}")
    private String accessSecret;

    @Value("${campus.jwt.access-expiration}")
    private long accessExpiration;

    @Value("${campus.jwt.refresh-secret}")
    private String refreshSecret;

    @Value("${campus.jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getAccessKey() {
        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey getRefreshKey() {
        return Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String role, Integer tokenVersion) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("role", role)
            .claim("tv", tokenVersion)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessExpiration))
            .signWith(getAccessKey())
            .compact();
    }

    public String generateRefreshToken(Long userId, String jti, Integer tokenVersion) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("jti", jti)
            .claim("tv", tokenVersion)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getRefreshKey())
            .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
            .verifyWith(getAccessKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public Claims parseRefreshToken(String token) {
        return Jwts.parser()
            .verifyWith(getRefreshKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateAccessToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            parseRefreshToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public Long getUserIdFromAccessToken(String token) {
        return Long.valueOf(parseAccessToken(token).getSubject());
    }

    public Long getUserIdFromRefreshToken(String token) {
        return Long.valueOf(parseRefreshToken(token).getSubject());
    }

    public String getRoleFromAccessToken(String token) {
        return parseAccessToken(token).get("role", String.class);
    }

    public Integer getTvFromAccessToken(String token) {
        return parseAccessToken(token).get("tv", Integer.class);
    }

    public String getJtiFromRefreshToken(String token) {
        return parseRefreshToken(token).get("jti", String.class);
    }

    public Integer getTvFromRefreshToken(String token) {
        return parseRefreshToken(token).get("tv", Integer.class);
    }
}
