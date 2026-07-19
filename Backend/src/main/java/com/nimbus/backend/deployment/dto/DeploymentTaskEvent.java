package com.nimbus.backend.deployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeploymentTaskEvent {

    private Long deploymentId;
    private String projectUuid;

    private String imageName;
    private String containerName;

    private String gitRepoUrl;
    private String branch;

    private Map<String, String> environmentVariables;
}