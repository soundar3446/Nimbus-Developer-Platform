package com.nimbus.backend.github.service.impl;

import com.nimbus.backend.github.service.OAuthStateService;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OAuthStateServiceImpl implements OAuthStateService {

    // Self-expiring cache: Automatically drops state tokens after 10 minutes
    private final Cache<String, Long> stateCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(5000)
            .build();

    @Override
    public String generateAndStoreState(Long userId) {
        String state = UUID.randomUUID().toString().replace("-", "");
        stateCache.put(state, userId);
        return state;
    }

    @Override
    public Optional<Long> getUserIdAndEvict(String state) {
        Long userId = stateCache.getIfPresent(state);
        if (userId != null) {
            stateCache.invalidate(state); // 🔥 Critical: Destroys token after one use (prevents replay attacks)
            return Optional.of(userId);
        }
        return Optional.empty();
    }
}