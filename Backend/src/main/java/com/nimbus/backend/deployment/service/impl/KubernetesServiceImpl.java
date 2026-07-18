package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.dto.DeploymentSummaryDto;
import com.nimbus.backend.deployment.service.KubernetesService;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesServiceImpl implements KubernetesService {

    private final AppsV1Api appsV1Api;
    private final CoreV1Api coreV1Api;
    private final NetworkingV1Api networkingV1Api;

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

  public V1Service createClusterIPService(String deploymentName, int targetPort) throws Exception {
        log.info("Generating ClusterIP Service definition for app target: {}", deploymentName);

        V1Service service = new V1Service();
        service.setApiVersion("v1");
        service.setKind("Service");

        // 1. Configure standard metadata block
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(deploymentName);
        metadata.setLabels(Map.of("app", deploymentName));
        service.setMetadata(metadata);

        // 2. Configure network selector specifications
        V1ServiceSpec spec = new V1ServiceSpec();
        spec.setType("ClusterIP");
        spec.setSelector(Map.of("app", deploymentName));

        // 3. Define the routing target port footprint
        V1ServicePort port = new V1ServicePort();
        port.setPort(80);
        port.setTargetPort(new IntOrString(targetPort));
        spec.setPorts(Collections.singletonList(port));
        
        service.setSpec(spec);

        // 4. Dispatch transaction payload directly to control plane
        V1Service deployedService = coreV1Api.createNamespacedService("default", service).execute();

        log.info("Service successfully instantiated. Internal DNS: {}.default.svc.cluster.local", deploymentName);
        return deployedService;
    }

    @Override
    public DeploymentSummaryDto getDeploymentSummary(String deploymentName) {
        String namespace = "default";
        DeploymentSummaryDto.DeploymentSummaryDtoBuilder builder = DeploymentSummaryDto.builder()
                .deploymentName(deploymentName)
                .namespace(namespace);

        try {
            // 1. Fetch Service network settings
            V1Service service = coreV1Api.readNamespacedService(deploymentName, namespace).execute();
            if (service.getSpec() != null) {
                builder.serviceName(deploymentName)
                        .clusterIP(service.getSpec().getClusterIP())
                        .internalDns(deploymentName + "." + namespace + ".svc.cluster.local");
            }

            // 2. Scan active Pod instances linked to this app label Selector
            V1PodList podList = coreV1Api.listNamespacedPod(namespace)
                    .labelSelector("app=" + deploymentName)
                    .execute();

            List<DeploymentSummaryDto.PodDetails> podDetailsList = new ArrayList<>();
            if (podList.getItems() != null) {
                for (V1Pod pod : podList.getItems()) {
                    String name = pod.getMetadata() != null ? pod.getMetadata().getName() : "Unknown";
                    String ip = pod.getStatus() != null ? pod.getStatus().getPodIP() : "Pending";
                    String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : "Unknown";

                    podDetailsList.add(DeploymentSummaryDto.PodDetails.builder()
                            .podName(name)
                            .podIP(ip)
                            .phase(phase)
                            .build());
                }
            }
            builder.pods(podDetailsList);

        } catch (Exception e) {
            log.warn("Failed collecting full telemetry summary details for: {}", deploymentName, e);
        }

        return builder.build();
    }

    @Override
    public String fetchLogs(String deploymentName) {
        String namespace = "default";
        try {
            V1PodList podList = coreV1Api.listNamespacedPod(namespace)
                    .labelSelector("app=" + deploymentName)
                    .execute();

            if (podList.getItems() != null && !podList.getItems().isEmpty()) {
                // Target the most recent pod instance matching the app selector
                V1Pod brokenPod = podList.getItems().get(0);
                String podName = brokenPod.getMetadata().getName();

                log.info("Retrieving container runtime logs from failing pod target: {}", podName);
                return coreV1Api.readNamespacedPodLog(podName, namespace)
                        .execute();
            }
        } catch (Exception e) {
            log.error("Failed downstream extraction log cycle for: {}", deploymentName, e);
        }
        return "No container failure logs could be retrieved from the cluster.";
    }

    public V1Ingress createApplicationIngress(String deploymentName) throws Exception {
        log.info("Generating NGINX Ingress routing rules for app target: {}", deploymentName);
        String namespace = "default";

        // 1. Map target backend port target values
        V1ServiceBackendPort backendPort = new V1ServiceBackendPort().number(80);
        V1IngressServiceBackend serviceBackend = new V1IngressServiceBackend()
                .name(deploymentName)
                .port(backendPort);

        V1IngressBackend backend = new V1IngressBackend().service(serviceBackend);

        // 2. Configure HTTP Path Routing structure (Prefix Matching)
        V1HTTPIngressPath ingressPath = new V1HTTPIngressPath()
                .path("/apps/" + deploymentName)
                .pathType("Prefix")
                .backend(backend);

        V1HTTPIngressRuleValue httpRuleValue = new V1HTTPIngressRuleValue()
                .paths(Collections.singletonList(ingressPath));

        V1IngressRule rule = new V1IngressRule().http(httpRuleValue);

        // 3. Assemble Spec and Add Standard Controller Routing Annotations
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(deploymentName);
        metadata.setLabels(Map.of("app", deploymentName));

        // Crucial for telling the local cluster which routing controller executes the manifest
        metadata.setAnnotations(Map.of(
                "kubernetes.io/ingress.class", "nginx",
                "nginx.ingress.kubernetes.io/rewrite-target", "/"
        ));

        V1IngressSpec spec = new V1IngressSpec()
                .rules(Collections.singletonList(rule));

        V1Ingress ingressManifest = new V1Ingress()
                .apiVersion("networking.k8s.io/v1")
                .kind("Ingress")
                .metadata(metadata)
                .spec(spec);

        return networkingV1Api.createNamespacedIngress(namespace, ingressManifest).execute();
    }

    @Override
    public Optional<V1Deployment> getDeployment(String deploymentName) {
        String namespace = "default";
        try {
            V1Deployment deployment = appsV1Api.readNamespacedDeployment(deploymentName, namespace).execute();
            return Optional.of(deployment);
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                log.info("Target deployment object [ {} ] not found in namespace: {}", deploymentName, namespace);
                return Optional.empty();
            }
            log.error("Systemic error querying cluster control plane for deployment: {}", deploymentName, e);
            throw new RuntimeException("Failed to read deployment from cluster plane", e);
        } catch (Exception e) {
            log.error("Unexpected error querying cluster for deployment: {}", deploymentName, e);
            throw new RuntimeException("Unexpected error reading deployment", e);
        }
    }

    @Override
    public void updateDeploymentReplicas(V1Deployment k8sDep, int replicas) throws Exception {
        String namespace = "default";

        if (k8sDep != null && k8sDep.getSpec() != null && k8sDep.getMetadata() != null) {
            String deploymentName = k8sDep.getMetadata().getName();
            k8sDep.getSpec().setReplicas(replicas);

            log.info("Scaling cluster deployment [ {} ] to {} replicas directly...", deploymentName, replicas);

            // Execute the patch replacement without fetching it again
            appsV1Api.replaceNamespacedDeployment(deploymentName, namespace, k8sDep)
                    .execute();
        }
    }

    @Override
    public void rolloutRestartDeployment(V1Deployment k8sDep) throws Exception {
        String namespace = "default";

        if (k8sDep != null && k8sDep.getSpec() != null && k8sDep.getSpec().getTemplate() != null) {
            String deploymentName = k8sDep.getMetadata().getName();
            V1PodTemplateSpec template = k8sDep.getSpec().getTemplate();

            if (template.getMetadata() == null) {
                template.setMetadata(new io.kubernetes.client.openapi.models.V1ObjectMeta());
            }

            Map<String, String> annotations = template.getMetadata().getAnnotations();
            if (annotations == null) {
                annotations = new HashMap<>();
            }
            annotations.put("nimbus.io/restartedAt", Instant.now().toString());
            template.getMetadata().setAnnotations(annotations);

            log.info("Triggering Kubernetes rolling rollout restart execution for: {}", deploymentName);

            appsV1Api.replaceNamespacedDeployment(deploymentName, namespace, k8sDep)
                    .execute();
        }
    }
}