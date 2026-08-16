package com.nimbus.backend.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // 1. Skip filter if Authorization header is missing or doesn't start with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract JWT token payload substring
        jwt = authHeader.substring(7);
        
        // 2.5 Check if token is blacklisted (logged out)
        if (tokenBlacklistService.isBlacklisted(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        userEmail = jwtService.extractUsername(jwt);

        // 3. Process validation if email exists and user isn't already authenticated in this thread context
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // OPTIMIZATION: Avoid hitting the database on every single API request. 
            // The JWT signature is already verified, so we can trust the email payload.
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(userEmail)
                    .password("") // Password not needed for stateless JWT validation
                    .authorities(java.util.Collections.emptyList())
                    .build();

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 4. Authenticate the user for the lifetime of this request thread
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // 5. Hand off execution to the next filter in the security pipeline
        filterChain.doFilter(request, response);
    }
}
