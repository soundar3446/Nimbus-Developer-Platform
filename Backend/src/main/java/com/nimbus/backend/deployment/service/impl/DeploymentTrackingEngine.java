package com.nimbus.backend.deployment.service.impl;

import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentTrackingEngine {

    private final AppsV1Api appsV1Api;

    public boolean waitUntilDeploymentReady(String deploymentName, int maxTimeoutSeconds) throws InterruptedException {
        log.info("Initiating structural readiness tracking for deployment: {}", deploymentName);
        
        int elapsed = 0;
        int pollInterval = 3;

        while (elapsed < maxTimeoutSeconds) {
            try {
                V1Deployment deployment = appsV1Api.readNamespacedDeployment(deploymentName, "default").execute();
                
                Integer desiredReplicas = deployment.getSpec().getReplicas();
                Integer availableReplicas = deployment.getStatus().getAvailableReplicas();
                Integer readyReplicas = deployment.getStatus().getReadyReplicas();

                log.info("Polling {} status -> Desired: {}, Ready: {}, Available: {}", 
                        deploymentName, desiredReplicas, readyReplicas, availableReplicas);

                // Verify that all requested replicas are officially live, healthy, and serving traffic
                if (desiredReplicas != null && desiredReplicas.equals(readyReplicas) && desiredReplicas.equals(availableReplicas)) {
                    log.info("Deployment {} successfully reached target saturation scale.", deploymentName);
                    return true;
                }

            } catch (Exception e) {
                log.warn("Failed fetching deployment metrics sheet, retrying cycle. Error: {}", e.getMessage());
            }

            Thread.sleep(pollInterval * 1000L);
            elapsed += pollInterval;
        }

        log.error("Deployment readiness tracking exceeded timeout threshold of {} seconds.", maxTimeoutSeconds);
        return false;
    }
}