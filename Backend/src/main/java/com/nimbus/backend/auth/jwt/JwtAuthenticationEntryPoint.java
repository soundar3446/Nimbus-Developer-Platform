package com.nimbus.backend.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nimbus.backend.common.dto.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        // 1. Force the response headers to JSON format and 401 Unauthorized status code
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 2. Construct our unified API envelope payload
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                false,
                "Unauthorized: You must present a valid token to access this resource. Details: " + authException.getMessage(),
                null
        );

        // 3. Manually map the record into a raw output JSON string using Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}