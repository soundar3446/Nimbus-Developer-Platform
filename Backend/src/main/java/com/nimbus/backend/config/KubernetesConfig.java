package com.nimbus.backend.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import java.io.File;

@Slf4j
@org.springframework.context.annotation.Configuration
public class KubernetesConfig {

    @Bean
    public ApiClient apiClient() {
        ApiClient client;

        File serviceAccountPath = new File("/var/run/secrets/kubernetes.io/serviceaccount");

        if (serviceAccountPath.exists()) {
            log.info("Production Mode: Detecting in-cluster service account token mapping footprints.");
            try {
                client = Config.fromCluster();
            } catch (Exception e) {
                log.error("Failed to load in-cluster configuration, parsing default fallback", e);
                client = new ApiClient();
            }
        } else {
            log.info("Development Mode: Parsing mounted local kubeconfig file...");
            try {
                client = Config.defaultClient();
                String originalPath = client.getBasePath();
                log.info("Initialized cluster API server path target: {}", originalPath);

                if (originalPath.contains("localhost") || originalPath.contains("127.0.0.1")) {
                    String updatedPath = originalPath
                            .replace("localhost", "host.docker.internal")
                            .replace("127.0.0.1", "host.docker.internal");
                    
                    client.setBasePath(updatedPath);
                    client.setVerifyingSsl(false);
                    log.info("Successfully patched networking path loop to host destination: {}", updatedPath);
                }
            } catch (Exception e) {
                log.error("CRITICAL: Failed to parse mounted kubeconfig file. Reason: ", e);
                // Hard fallback to force it away from 8080 if file system reading fails
                client = new ApiClient();
                client.setBasePath("https://host.docker.internal:6443");
                client.setVerifyingSsl(false);
            }
        }

        // Disable strict JSON validation to handle newer cluster versions gracefully
        if (client.getJSON() != null) {
            client.getJSON().setLenientOnJson(true);
        }

         log.debug("Kubernetes API Base URL: {}", client.getBasePath());
         log.debug("Authentication methods: {}", client.getAuthentications().keySet());

        Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public NetworkingV1Api networkingV1Api(ApiClient apiClient) { return new NetworkingV1Api(apiClient); }
}