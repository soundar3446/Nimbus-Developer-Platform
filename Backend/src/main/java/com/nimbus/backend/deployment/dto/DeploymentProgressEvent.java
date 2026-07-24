package com.nimbus.backend.deployment.dto;

import com.nimbus.backend.deployment.enums.DeploymentStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentProgressEvent {
    private Long deploymentId;
    private String projectUuid;
    private DeploymentStatus status;
    private int progressPercentage;
    private String logLine;
    private long timestamp;
}