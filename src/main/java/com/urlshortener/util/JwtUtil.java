package com.urlshortener.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;


/**
 * Utility class for handling JWT token operations.
 * <p>
 * Responsibilities:
 * - Generate JWT tokens
 * - Extract claims from tokens
 * - Validate tokens
 * - Extract username from token
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;

    private Key signingKey;

    /**
     * Initializes signing key after properties are injected.
     * Decodes base64 secret and builds HMAC signing key.
     */
    @PostConstruct
    private void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates JWT token for the given username.
     *
     * @param username authenticated user
     * @return signed JWT token
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts claims from JWT token.
     *
     * @param token JWT token
     * @return claims if valid, otherwise null
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts username (subject) from token.
     *
     * @param token JWT token
     * @return username if valid, otherwise null
     */
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Validates token by checking if claims can be extracted.
     *
     * @param token JWT token
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        return extractClaims(token) != null;
    }
}
