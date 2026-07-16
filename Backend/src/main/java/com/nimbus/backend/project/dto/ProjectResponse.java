package com.nimbus.backend.project.dto;

import com.nimbus.backend.project.enums.ProjectStatus;
import lombok.*;

import java.time.Instant;

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
}