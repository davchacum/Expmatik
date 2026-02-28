package com.expmatik.backend.auth;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expirationDate) {
        blacklistedTokens.put(token, expirationDate);
        System.out.println("=== Token agregado a blacklist ===");
        System.out.println("Token: " + token.substring(0, Math.min(20, token.length())) + "...");
        System.out.println("Expira: " + expirationDate);
        System.out.println("Total tokens en blacklist: " + blacklistedTokens.size());
        cleanupExpiredTokens();
    }

    public boolean isTokenBlacklisted(String token) {
        cleanupExpiredTokens();
        boolean isBlacklisted = blacklistedTokens.containsKey(token);
        System.out.println("Verificando token en blacklist: " + isBlacklisted + " (Total: " + blacklistedTokens.size() + ")");
        return isBlacklisted;
    }

    private void cleanupExpiredTokens() {
        Date now = new Date();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().before(now));
    }

    public int getBlacklistedTokensCount() {
        cleanupExpiredTokens();
        return blacklistedTokens.size();
    }

    public void clearAll() {
        blacklistedTokens.clear();
    }
}
