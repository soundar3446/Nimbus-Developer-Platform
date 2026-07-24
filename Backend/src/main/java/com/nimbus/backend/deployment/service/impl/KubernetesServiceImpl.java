package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.dto.DeploymentSummaryDto;
import com.nimbus.backend.deployment.service.KubernetesService;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.ApiResponse;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Service;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class KubernetesServiceImpl implements KubernetesService {

    private final AppsV1Api appsV1Api;
    private final CoreV1Api coreV1Api;
    private final NetworkingV1Api networkingV1Api;

    @Override
    public V1Deployment deployApplication(String deploymentName, String imageName, int targetExposedPort, Map<String, String> envVariables) throws Exception {
        String namespace = "default";
        Map<String, String> labels = Map.of("app", deploymentName);
        String secretName = deploymentName + "-secrets";
        String registrySecretName = "nimbus-reg-secret-" + deploymentName;

        String pullPolicy = (imageName.contains("/") || imageName.contains(".")) ? "Always" : "IfNotPresent";

        V1Container container = new V1Container()
                .name(deploymentName + "-container")
                .image(imageName)
                .imagePullPolicy(pullPolicy)
                .ports(Collections.singletonList(new V1ContainerPort().containerPort(targetExposedPort)));

        if (envVariables != null && !envVariables.isEmpty()) {
            createOrUpdateNamespacedSecret(secretName, envVariables);

            List<V1EnvVar> secureK8sEnvVars = new ArrayList<>();
            envVariables.keySet().forEach(key -> {
                secureK8sEnvVars.add(new V1EnvVar()
                        .name(key)
                        .valueFrom(new io.kubernetes.client.openapi.models.V1EnvVarSource()
                                .secretKeyRef(new io.kubernetes.client.openapi.models.V1SecretKeySelector()
                                        .name(secretName)
                                        .key(key))));
            });

            container.env(secureK8sEnvVars);
            log.info("Injected {} custom environment variables into pod template specs for: {}", secureK8sEnvVars.size(), deploymentName);
        }

        V1PodSpec podSpec = new V1PodSpec()
                .containers(Collections.singletonList(container));

        // Attach K8s docker-registry imagePullSecrets to pod spec if registry secret was registered
        try {
            coreV1Api.readNamespacedSecret(registrySecretName, namespace).execute();
            podSpec.setImagePullSecrets(Collections.singletonList(
                    new io.kubernetes.client.openapi.models.V1LocalObjectReference().name(registrySecretName)
            ));
            log.info("Attached imagePullSecrets reference [{}] to pod template spec", registrySecretName);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                log.warn("Could not inspect imagePullSecrets existence for {}, proceeding without pull secrets", registrySecretName);
            }
        }

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

    @Override
    public void createApplicationIngress(String deploymentName, String subdomain, String customDomain, boolean isCustomDomainVerified) throws Exception {
        String namespace = "default";
        String ingressName = deploymentName + "-ingress";
        String baseDomain = "nimbus.app"; // System root domain

        List<V1IngressRule> rules = new ArrayList<>();

        // 1. Default Subdomain Host (e.g., my-app.nimbus.app)
        String defaultHost = (subdomain != null && !subdomain.isBlank())
                ? subdomain + "." + baseDomain
                : deploymentName + "." + baseDomain;

        rules.add(buildIngressRule(defaultHost, deploymentName, 8080));

        // 2. Custom Domain Host (if verified)
        if (customDomain != null && !customDomain.isBlank() && isCustomDomainVerified) {
            rules.add(buildIngressRule(customDomain, deploymentName, 8080));
            log.info("Attached verified custom domain [{}] to Ingress: {}", customDomain, ingressName);
        }

        V1IngressSpec ingressSpec = new V1IngressSpec()
                .ingressClassName("nginx") // Standard NGINX Ingress Controller
                .rules(rules);

        V1Ingress ingress = new V1Ingress()
                .apiVersion("networking.k8s.io/v1")
                .kind("Ingress")
                .metadata(new V1ObjectMeta()
                        .name(ingressName)
                        .putAnnotationsItem("nginx.ingress.kubernetes.io/ssl-redirect", "false")) // Handled in Phase 9 with Cert-Manager
                .spec(ingressSpec);

        try {
            networkingV1Api.createNamespacedIngress(namespace, ingress).execute();
            log.info("Created K8s Ingress routing for host: {}", defaultHost);
        } catch (ApiException e) {
            if (e.getCode() == 409) { // 409 Conflict -> Replace existing rule
                networkingV1Api.replaceNamespacedIngress(ingressName, namespace, ingress).execute();
                log.info("Updated existing K8s Ingress routing: {}", ingressName);
            } else {
                throw e;
            }
        }
    }

    // Helper method to construct Ingress Rule paths
    private V1IngressRule buildIngressRule(String host, String serviceName, int servicePort) {
        return new V1IngressRule()
                .host(host)
                .http(new V1HTTPIngressRuleValue()
                        .addPathsItem(new V1HTTPIngressPath()
                                .path("/")
                                .pathType("Prefix")
                                .backend(new V1IngressBackend()
                                        .service(new V1IngressServiceBackend()
                                                .name(serviceName)
                                                .port(new V1ServiceBackendPort().number(servicePort))))));
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

    @Override
    public void updateDeploymentImage(String deploymentName, String newImageTag, Map<String, String> envVariables) throws Exception {
        String namespace = "default";
        String secretName = deploymentName + "-secrets";

        // 1. Refresh secret items inside the cluster framework first
        if (envVariables != null && !envVariables.isEmpty()) {
            createOrUpdateNamespacedSecret(secretName, envVariables);
        }

        log.info("Constructing secret-aware atomic JSON Merge Patch for zero-downtime rolling update on: {}", deploymentName);

        // 2. Build out the updated environment source variable reference list
        List<Map<String, Object>> secureEnvPatchList = new ArrayList<>();
        if (envVariables != null && !envVariables.isEmpty()) {
            envVariables.keySet().forEach(key -> {
                Map<String, Object> secretKeyRef = new HashMap<>();
                secretKeyRef.put("name", secretName);
                secretKeyRef.put("key", key);

                Map<String, Object> valueFrom = new HashMap<>();
                valueFrom.put("secretKeyRef", secretKeyRef);

                Map<String, Object> envEntry = new HashMap<>();
                envEntry.put("name", key);
                envEntry.put("valueFrom", valueFrom);

                secureEnvPatchList.add(envEntry);
            });
        }

        // 3. Assemble the atomic json patch delta map matching the exact K8s Spec path structure
        Map<String, Object> containerDelta = new HashMap<>();
        containerDelta.put("name", deploymentName + "-container");
        containerDelta.put("image", newImageTag);
        if (!secureEnvPatchList.isEmpty()) {
            containerDelta.put("env", secureEnvPatchList);
        }

        Map<String, Object> podSpecDelta = new HashMap<>();
        podSpecDelta.put("containers", List.of(containerDelta));

        Map<String, Object> templateSpecDelta = new HashMap<>();
        templateSpecDelta.put("spec", podSpecDelta);

        Map<String, Object> deploymentSpecDelta = new HashMap<>();
        deploymentSpecDelta.put("template", templateSpecDelta);

        Map<String, Object> patchMap = new HashMap<>();
        patchMap.put("spec", deploymentSpecDelta);

        // 4. Convert our delta map into a structural V1Patch wrapper
        String jsonPatchString = new com.google.gson.Gson().toJson(patchMap);
        io.kubernetes.client.custom.V1Patch jsonMergePatch = new io.kubernetes.client.custom.V1Patch(jsonPatchString);

        // 5. Fire the atomic patch transaction targeting the cluster control plane
        log.info("Patching Kubernetes deployment [ {} ] container image reference via JSON Merge Patch to -> {}", deploymentName, newImageTag);

        io.kubernetes.client.openapi.ApiClient client = appsV1Api.getApiClient();

        // Build out the explicit REST route endpoint matching the cluster routing path exactly
        String path = "/apis/apps/v1/namespaces/" + client.escapeString(namespace)
                + "/deployments/" + client.escapeString(deploymentName);

        // Configure headers manually to bypass the generated code's 415 media type constraint flaws
        Map<String, String> headerParams = new HashMap<>();
        headerParams.put("Accept", "application/json");
        headerParams.put("Content-Type", "application/merge-patch+json"); // Forces correct patch media header format

        // Fire the transaction through the native HTTP engine frame via public builder layout calls
        okhttp3.Call rawHttpCall = client.buildCall(
                null,                        // 1. basePath (defaults to cluster context url)
                path,                        // 2. REST path
                "PATCH",                     // 3. HTTP Verb method
                new ArrayList<>(),           // 4. queryParams
                new ArrayList<>(),           // 5. collectionQueryParams
                jsonMergePatch,              // 6. V1 Patch payload body
                headerParams,                // 7. headerParams (manual content-type override)
                new HashMap<>(),             // 8. cookieParams
                new HashMap<>(),             // 9. formParams
                new String[]{"BearerToken"}, // 10. authNames
                null                         // 11. callback listener
        );

        // Execute the patch call transaction and process structural execution failures
        try (okhttp3.Response response = rawHttpCall.execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Empty body response";
                log.error("Cluster API patch execution rejection footprint: Code: {} | Body: {}", response.code(), errorBody);
                throw new io.kubernetes.client.openapi.ApiException(
                        "Patch transactional failure processing manifest: " + errorBody,
                        response.code(),
                        null,
                        null
                );
            }
        }

        log.info("Successfully pushed secret-aware atomic patch to cluster plane for zero-downtime rolling updates.");
    }

    @Override
    public V1Secret createOrUpdateNamespacedSecret(String secretName, Map<String, String> rawSecrets) throws Exception {
        String namespace = "default";

        // 1. Standardize text strings to Base64 byte layouts for Kubernetes secret compatibility
        Map<String, byte[]> encodedData = new HashMap<>();
        if (rawSecrets != null) {
            rawSecrets.forEach((key, value) -> {
                if (value != null) {
                    encodedData.put(key, value.getBytes(StandardCharsets.UTF_8));
                }
            });
        }

        // 2. Assemble the structural V1Secret declaration footprint
        V1Secret secretManifest = new V1Secret()
                .apiVersion("v1")
                .kind("Secret")
                .metadata(new V1ObjectMeta()
                        .name(secretName)
                        .namespace(namespace))
                .type("Opaque")
                .data(encodedData);

        // 3. Perform a safe transactional Upsert routine inside the namespace
        try {
            log.info("Creating a secure Kubernetes Secret artifact object mapping for: {}", secretName);
            return coreV1Api.createNamespacedSecret(namespace, secretManifest).execute();
        } catch (ApiException e) {
            if (e.getCode() == 409) { // Conflict -> Object already exists, perform a safe update patch instead
                log.info("Secret footprint already exists for {}. Executing rolling update patch transaction...", secretName);
                return coreV1Api.replaceNamespacedSecret(secretName, namespace, secretManifest).execute();
            }
            log.error("Failed to construct cloud-native secret environment mapping for: {}", secretName, e);
            throw e;
        }
    }

    @Override
    public void streamPodLogs(String deploymentName, Consumer<String> logConsumer) {
        try {
            // 1. Fetch active pod using fluent list call
            V1PodList podList = coreV1Api.listNamespacedPod("default")
                    .labelSelector("app=" + deploymentName)
                    .execute();

            if (podList.getItems() == null || podList.getItems().isEmpty()) {
                logConsumer.accept("No active pods found for deployment: " + deploymentName);
                return;
            }

            String podName = podList.getItems().get(0).getMetadata().getName();

            // 2. Execute with HTTP Info to get the raw ApiResponse container
            ApiResponse<String> apiResponse = coreV1Api.readNamespacedPodLog(podName, "default")
                    .tailLines(100)
                    .follow(true)
                    .executeWithHttpInfo();

            // 3. Read raw stream from the underlying OkHttp client call
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    apiResponse.getData() != null ?
                            new java.io.ByteArrayInputStream(apiResponse.getData().getBytes()) :
                            java.io.InputStream.nullInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    logConsumer.accept(line);
                }
            }
        } catch (Exception e) {
            log.error("Error streaming pod logs for {}", deploymentName, e);
            logConsumer.accept("Error reading log stream: " + e.getMessage());
        }
    }

    public void createDockerRegistrySecret(String secretName, String registryServer, String username, String password) throws Exception {
        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        String dockerConfigJson = String.format(
                "{\"auths\":{\"%s\":{\"username\":\"%s\",\"password\":\"%s\",\"auth\":\"%s\"}}}",
                registryServer, username, password, encodedAuth
        );

        // 1. Set Metadata
        V1ObjectMeta metadata = new V1ObjectMeta();
        metadata.setName(secretName);
        metadata.setNamespace("default");

        // 2. Set Data Payload (Kubernetes expects byte[] values mapped to key names)
        Map<String, byte[]> dataMap = new HashMap<>();
        dataMap.put(".dockerconfigjson", dockerConfigJson.getBytes());

        // 3. Assemble V1Secret POJO
        V1Secret secret = new V1Secret();
        secret.setApiVersion("v1");
        secret.setKind("Secret");
        secret.setMetadata(metadata);
        secret.setType("kubernetes.io/dockerconfigjson");
        secret.setData(dataMap);

        // 4. Create or Replace Secret in Kubernetes
        try {
            coreV1Api.createNamespacedSecret("default", secret).execute();
            log.info("Created K8s docker-registry secret: {}", secretName);
        } catch (ApiException e) {
            if (e.getCode() == 409) { // 409 Conflict -> Replace existing
                coreV1Api.replaceNamespacedSecret(secretName, "default", secret).execute();
                log.info("Updated existing K8s docker-registry secret: {}", secretName);
            } else {
                throw e;
            }
        }
    }
}