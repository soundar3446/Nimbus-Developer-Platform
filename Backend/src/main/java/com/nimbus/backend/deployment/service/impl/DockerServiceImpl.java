package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.service.DockerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
public class DockerServiceImpl implements DockerService {

    @Override
    public String runContainer(String containerName, int hostPort, int targetExposedPort, String imageName) throws Exception {
        // Run container in detached mode (-d)
        ProcessBuilder pb = new ProcessBuilder(
                "docker", "run", "-d",
                "--name", containerName,
                "-p", hostPort + ":" + targetExposedPort,
                imageName + ":latest"
        );

        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line.trim());
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Docker daemon failed to spin up container runtime engine blueprint.");
        }

        // Docker run -d returns the full 64-character container ID string
        return output.toString();
    }

    @Override
    public void stopAndRemoveContainer(String containerName) {
        try {
            executeSilentCommand("docker", "stop", containerName);
            executeSilentCommand("docker", "rm", containerName);
            log.info("🗑️ Context container resources purged successfully: {}", containerName);
        } catch (Exception e) {
            log.warn("Non-fatal cleanup exception clearing container environment context", e);
        }
    }

    @Override
    public boolean pollHealthCheck(String urlString, int maxRetries, int delayMillis) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);

                int responseCode = connection.getResponseCode();
                // Accept basic 200 OK or standard server index responses as healthy for an MVP
                if (responseCode >= 200 && responseCode < 400) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("Health check polling pass {} failed for destination: {}", i + 1, urlString);
            }

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private void executeSilentCommand(String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();
        p.waitFor();
    }
}