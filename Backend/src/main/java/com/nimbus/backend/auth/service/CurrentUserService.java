package com.nimbus.backend.auth.service;

import com.nimbus.backend.user.entity.User;

import java.util.Optional;

public interface CurrentUserService {

    /**
     * Retrieves the currently authenticated User entity from the security context.
     * Throws an exception if the user is not authenticated.
     */
    User getCurrentUser();

    /**
     * Retrieves the currently authenticated User entity if present.
     */
    Optional<User> getCurrentUserOptional();

    /**
     * Retrieves the email address string of the active authenticated session.
     */
    String getCurrentUserEmail();
}