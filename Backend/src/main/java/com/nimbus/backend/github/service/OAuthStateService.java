package com.nimbus.backend.github.service;

import java.util.Optional;

public interface OAuthStateService {
    String generateAndStoreState(Long userId);
    Optional<Long> getUserIdAndEvict(String state);
}