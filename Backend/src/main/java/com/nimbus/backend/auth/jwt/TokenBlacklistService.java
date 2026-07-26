package com.nimbus.backend.auth.jwt;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A lightweight in-memory token blacklist for handling logouts.
 * In a production/enterprise environment, this should be backed by Redis 
 * or a database to survive application restarts and scale across multiple instances.
 */
@Service
public class TokenBlacklistService {

    // Using a ConcurrentHashMap keySet to ensure thread-safe operations
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    /**
     * Adds a token to the blacklist, effectively revoking it.
     */
    public void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    /**
     * Checks if a token has been blacklisted.
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}
