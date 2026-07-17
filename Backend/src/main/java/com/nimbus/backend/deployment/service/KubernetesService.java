package com.nimbus.backend.deployment.service;

import io.kubernetes.client.openapi.models.V1Deployment;

public interface KubernetesService {
    V1Deployment deployApplication(String deploymentName, String imageName, int targetExposedPort) throws Exception;

    boolean verifyPodIsRunning(String deploymentName, int maxRetries, int sleepMillis);
}
