package com.nimbus.backend.deployment.dto;

import com.nimbus.backend.deployment.enums.DeploymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentResponseDto {
    private Long id;
    private String projectUuid;
    private DeploymentStatus status;
    private String gitCommitHash;
    private String branch;
    private String imageTag;
    private Long durationMs;
    private String containerName;
    private String applicationUrl;
    private Instant createdAt;
}