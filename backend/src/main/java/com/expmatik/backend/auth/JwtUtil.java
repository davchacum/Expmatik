package com.expmatik.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret:mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLongForHS256Algorithm}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    public JwtUtil(TokenBlacklistService tokenBlacklistService) {
        this.tokenBlacklistService = tokenBlacklistService;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date()) || tokenBlacklistService.isTokenBlacklisted(token);
    }

    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }

    public String generateToken(String email, Map<String, Object> additionalClaims) {
        return createToken(additionalClaims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);
        
        System.out.println("=== Validando token ===");
        System.out.println("Email del token: " + email);
        System.out.println("Email del usuario: " + userDetails.getUsername());
        System.out.println("¿Token expirado?: " + isTokenExpired(token));
        System.out.println("¿Token en blacklist?: " + isBlacklisted);
        System.out.println("Tokens en blacklist: " + tokenBlacklistService.getBlacklistedTokensCount());
        
        return (email.equals(userDetails.getUsername()) 
                && !isTokenExpired(token)
                && !isBlacklisted);
    }

    public Boolean validateToken(String token) {
        try {
            boolean isExpired = isTokenExpired(token);
            boolean isBlacklisted = tokenBlacklistService.isTokenBlacklisted(token);
            
            System.out.println("=== Validando token (sin userDetails) ===");
            System.out.println("¿Token expirado?: " + isExpired);
            System.out.println("¿Token en blacklist?: " + isBlacklisted);
            
            return !isExpired && !isBlacklisted;
        } catch (Exception e) {
            System.out.println("Error validando token: " + e.getMessage());
            return false;
        }
    }
}
