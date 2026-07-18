package com.nimbus.backend.deployment.service;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

public interface KubernetesService {
    V1Deployment deployApplication(String deploymentName, String imageName, int targetExposedPort) throws Exception;

    boolean verifyPodIsRunning(String deploymentName, int maxRetries, int sleepMillis);
    V1Service createClusterIPService(String deploymentName, int targetPort) throws Exception;
}
