package com.nimbus.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Provide a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;
}