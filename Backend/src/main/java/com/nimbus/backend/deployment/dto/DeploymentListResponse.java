package com.nimbus.backend.deployment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentListResponse {
    private String deploymentName;
    private String namespace;
    private int replicas;
    private int readyReplicas;
    private String status;
    private String age;
    private List<String> podNames;
}