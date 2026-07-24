package com.nimbus.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    @NotBlank(message = "Project name cannot be blank")
    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    private String name;

    private String description;

    private String githubRepo;

    @Builder.Default
    private String defaultBranch = "main";

    @NotBlank(message = "DockerFile path is empty.")
    private String dockerfilePath;

    @NotBlank(message = "Build context path is empty.")
    private String contextPath;

    @NotBlank(message = "Image name must not be empty.")
    private String imageName;

    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain can only contain lowercase letters, numbers, and hyphens.")
    @Size(min = 3, max = 63, message = "Subdomain length must be between 3 and 63 characters.")
    private String subdomain;

    private String customDomain;

    private String registryUrl;
    private String registryUsername;
    private String registryToken;

    @Builder.Default
    private Map<String, String> environmentVariables = new HashMap<>();
}