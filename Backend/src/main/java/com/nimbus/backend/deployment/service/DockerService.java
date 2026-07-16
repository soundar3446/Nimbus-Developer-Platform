package com.nimbus.backend.deployment.service;

public interface DockerService {

    String runContainer(
            String containerName,
            int hostPort,
            int targetExposedPort,
            String imageName
    ) throws Exception;

    void stopAndRemoveContainer(String containerName);

    boolean pollHealthCheck(
            String urlString,
            int maxRetries,
            int delayMillis
    );
}