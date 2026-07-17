package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.service.KubernetesService;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesServiceImpl implements KubernetesService {

    private final AppsV1Api appsV1Api;
    private final CoreV1Api coreV1Api;

    @Override
    public V1Deployment deployApplication(String deploymentName, String imageName, int targetExposedPort) throws Exception {
        String namespace = "default";
        Map<String, String> labels = Map.of("app", deploymentName);

        V1Container container = new V1Container()
                .name(deploymentName + "-container")
                .image(imageName)
                .imagePullPolicy("IfNotPresent")
                .ports(Collections.singletonList(new V1ContainerPort().containerPort(targetExposedPort)));

        V1PodTemplateSpec templateSpec = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(new V1PodSpec().containers(Collections.singletonList(container)));

        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
                .replicas(1)
                .selector(new V1LabelSelector().matchLabels(labels))
                .template(templateSpec);

        V1Deployment manifest = new V1Deployment()
                .apiVersion("apps/v1")
                .kind("Deployment")
                .metadata(new V1ObjectMeta().name(deploymentName).labels(labels))
                .spec(deploymentSpec);

        log.info("sending compiled manifest footprint to cluster engine: {}", deploymentName);
        return appsV1Api.createNamespacedDeployment(namespace, manifest).execute();
    }

    @Override
    public boolean verifyPodIsRunning(String deploymentName, int maxRetries, int sleepMillis) {
        String namespace = "default";
        String labelSelector = "app=" + deploymentName;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // Notice the method call matches the official Java client definition format:
                V1PodList podList = coreV1Api.listNamespacedPod(namespace).execute();

                if (podList.getItems() != null && !podList.getItems().isEmpty()) {
                    V1Pod activePod = podList.getItems().get(0);
                    if (activePod.getStatus() != null) {
                        String currentPhase = activePod.getStatus().getPhase();
                        log.info("Polling Pod execution cluster phase metrics: [ {} ]", currentPhase);

                        if ("Running".equalsIgnoreCase(currentPhase)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed parsing cluster pod status matrices, retrying...", e);
            }

            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}