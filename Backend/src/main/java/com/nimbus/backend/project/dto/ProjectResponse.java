package com.nimbus.backend.project.dto;

import com.nimbus.backend.project.enums.ProjectStatus;
import lombok.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String uuid;
    private String name;
    private String description;
    private String githubRepo;
    private String defaultBranch;
    private Long ownerId;
    private String ownerEmail;
    private ProjectStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String dockerfilePath;
    private String contextPath;
    private String imageName;

    private String subdomain;
    private String defaultUrl;
    private String customDomain;
    private Boolean customDomainVerified;

    private String registryUrl;
    private String registryUsername;

    @Builder.Default
    private Map<String, String> environmentVariables = new HashMap<>();
}