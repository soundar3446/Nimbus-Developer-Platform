package com.nimbus.backend.config;

import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesConfig {
    private final AppsV1Api appsV1Api;

    public V1Deployment deployApplication(String deploymentName, String imageName, int targetExposedPort) throws Exception {
        String namespace = "default";
        Map<String, String> labels = Map.of("app", deploymentName);

        // 1. Define the Container Specifications
        V1Container container = new V1Container()
                .name(deploymentName + "-container")
                .image(imageName)
                .imagePullPolicy("IfNotPresent") // Useful for using local daemon images during Phase 1 testing
                .ports(Collections.singletonList(new V1ContainerPort().containerPort(targetExposedPort)));

        // 2. Define the Pod Spec Template
        V1PodTemplateSpec templateSpec = new V1PodTemplateSpec()
                .metadata(new V1ObjectMeta().labels(labels))
                .spec(new V1PodSpec().containers(Collections.singletonList(container)));

        // 3. Define the Deployment Spec (Selector configuration, Replicas matchers)
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec()
                .replicas(1) // Keep it at a steady baseline single replica instance for now
                .selector(new V1LabelSelector().matchLabels(labels))
                .template(templateSpec);

        // 4. Assemble the Root V1Deployment Object Manifest
        V1Deployment manifest = new V1Deployment()
                .apiVersion("apps/v1")
                .kind("Deployment")
                .metadata(new V1ObjectMeta().name(deploymentName).labels(labels))
                .spec(deploymentSpec);

        log.info("Sending compiled manifest footprint to cluster engine: {}", deploymentName);

        // 5. Fire SDK network request straight into the cluster API plane
        return appsV1Api.createNamespacedDeployment(
                namespace,
                manifest
                // pretty
                // dryRun
                // fieldManager
                // fieldValidation
        ).execute();
    }
}
