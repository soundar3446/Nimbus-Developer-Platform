package com.nimbus.backend.deployment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DeploymentSummaryDto {
    private String deploymentName;
    private String namespace;
    private int desiredReplicas;
    private int readyReplicas;
    private String serviceName;
    private String clusterIP;
    private String internalDns;
    private String status;
    private List<PodDetails> pods;

    @Data
    @Builder
    public static class PodDetails {
        private String podName;
        private String podIP;
        private String phase;
    }
}
