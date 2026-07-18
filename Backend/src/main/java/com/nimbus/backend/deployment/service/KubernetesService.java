package com.nimbus.backend.deployment.service;

import com.nimbus.backend.deployment.dto.DeploymentSummaryDto;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Ingress;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.Map;
import java.util.Optional;

public interface KubernetesService {
    V1Deployment deployApplication(String deploymentName, String imageName, int targetExposedPort, Map<String, String> envVariables) throws Exception;
    boolean verifyPodIsRunning(String deploymentName, int maxRetries, int sleepMillis);
    V1Service createClusterIPService(String deploymentName, int targetPort) throws Exception;
    DeploymentSummaryDto getDeploymentSummary(String deploymentName);
    String fetchLogs(String deploymentName);
    V1Ingress createApplicationIngress(String deploymentName) throws Exception;
    Optional<V1Deployment> getDeployment(String deploymentName);
    void updateDeploymentReplicas(V1Deployment deployment, int replicas) throws Exception;
    void rolloutRestartDeployment(V1Deployment deployment) throws Exception;
    void updateDeploymentImage(String deploymentName, String newImageTag, Map<String, String> envVariables) throws Exception;
}
