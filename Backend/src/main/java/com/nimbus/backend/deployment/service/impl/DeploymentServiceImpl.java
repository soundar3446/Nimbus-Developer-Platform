package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.deployment.service.*;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.enums.ProjectStatus;
import com.nimbus.backend.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentServiceImpl implements DeploymentService {

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final GitService gitService;
    private final DockerService dockerService;
    private final KubernetesService kubernetesService;
    private final DeploymentTrackingEngine deploymentTrackingEngine;

    @Override
    @Async("taskExecutor") // Runs asynchronously so the REST API returns immediately
    public void triggerDeploymentPipeline(String projectId) {

        log.info("Running on thread: {}", Thread.currentThread().getName());

        Project project = projectRepository.findByUuid(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project target not found"));

        // Initialize tracking record
        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.CLONING)
                .build();
        deployment = deploymentRepository.save(deployment);

        File workspace = null;
        String k8sDeploymentName = null;

        try {

            if(project.getOwner().getGithubIntegration() == null){
                throw new IllegalStateException("Project Owner does not have active GitHub Integration");
            }

            String token = project.getOwner().getGithubIntegration().getGithubAccessToken();
            workspace = gitService.cloneRepository(project.getGithubRepo(), token, project.getDefaultBranch());

            String dfRelativePath = (project.getDockerfilePath() != null && !project.getDockerfilePath().isBlank())
                    ? project.getDockerfilePath() : "Dockerfile";
            String contextRelativePath = (project.getContextPath() != null && !project.getContextPath().isBlank())
                    ? project.getContextPath() : ".";

            File absoluteDockerfile = new File(workspace, dfRelativePath);
            File absoluteBuildContext = new File(workspace, contextRelativePath);


            if (!absoluteDockerfile.exists() || !absoluteDockerfile.isFile()) {
                throw new IllegalArgumentException("Declared Dockerfile not found at workspace location path: " + dfRelativePath);
            }
            if (!absoluteBuildContext.exists() || !absoluteBuildContext.isDirectory()) {
                throw new IllegalArgumentException("Declared Build Context directory path not found: " + contextRelativePath);
            }

            String targetImage = project.getImageName() + ":latest";

            log.info("Running user-configured docker build...");
            log.info("   ↳ Dockerfile: {}", absoluteDockerfile.getAbsolutePath());
            log.info("   ↳ Context:    {}", absoluteBuildContext.getAbsolutePath());

            deployment.setStatus(DeploymentStatus.BUILDING);
            deploymentRepository.save(deployment);

            executeSystemCommand(
                    absoluteBuildContext, // Run the command *inside* the user's designated context directory
                    "docker", "build",
                    "-f", absoluteDockerfile.getAbsolutePath(),
                    "-t", targetImage,
                    "."
            );

            log.info("User application container successfully compiled: {}", targetImage);

            deployment.setStatus(DeploymentStatus.STARTING_CONTAINER);
            deploymentRepository.save(deployment);

            k8sDeploymentName = "nimbus-" + deployment.getId();
            int targetPort = 8080;

            log.info("Orchestrating Kubernetes Deployment object: {}", k8sDeploymentName);
            kubernetesService.deployApplication(k8sDeploymentName, targetImage, targetPort);

            log.info("Orchestrating Kubernetes Service object: {}", k8sDeploymentName);
            kubernetesService.createClusterIPService(k8sDeploymentName, targetPort);

            deployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            deployment.setContainerName(k8sDeploymentName);
            deploymentRepository.save(deployment);

            boolean isHealthy = kubernetesService.verifyPodIsRunning(k8sDeploymentName, 15, 3000);
            if (isHealthy) {
                log.info("Pod successfully scheduled and running in cluster! Promoting deployment status.");
                deployment.setStatus(DeploymentStatus.RUNNING);
                project.setStatus(ProjectStatus.CONNECTED);
            } else {
                log.error("Pod context compilation failed to reach a Running state within timeout limits.");
                deployment.setStatus(DeploymentStatus.FAILED);
            }

            deploymentRepository.save(deployment);
            projectRepository.save(project);

        } catch (Exception e) {
            log.error("Kubernetes engine deployment tracking layer failed on project UUID: {}", projectId, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
        } finally {
            if (workspace != null && workspace.exists()) {
                deleteDirectory(workspace);
            }
        }
    }

    @Override
    @Transactional
    public void startDeployment(Long id) {
        Deployment deployment = deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment target not found for ID: " + id));

        if (deployment.getStatus() == DeploymentStatus.RUNNING) {
            throw new IllegalStateException("Application deployment context is already active and running.");
        }

        try {
            log.info("Starting existing container layout: {}", deployment.getContainerName());

            // Re-fire the container using standard docker execution commands
            ProcessBuilder pb = new ProcessBuilder("docker", "start", deployment.getContainerName());
            Process process = pb.start();
            int exitCode = process.waitFor();

            // If the container was purged or missing, spin it up fresh using the existing properties
            if (exitCode != 0) {
                log.warn("Container footprint missing. Re-instantiating fresh container runtime layout.");
                String longContainerId = dockerService.runContainer(
                        deployment.getContainerName(),
                        deployment.getHostPort(),
                        8080, // Target exposed application port
                        deployment.getProject().getImageName()
                );
                deployment.setContainerId(longContainerId);
            }

            deployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            deploymentRepository.save(deployment);

            // Verify the restarted app is listening before marking it active
            boolean isHealthy = dockerService.pollHealthCheck(deployment.getApplicationUrl(), 5, 2000);
            if (isHealthy) {
                log.info("Application successfully back online at URL: {}", deployment.getApplicationUrl());
                deployment.setStatus(DeploymentStatus.RUNNING);
            } else {
                deployment.setStatus(DeploymentStatus.FAILED);
            }
            deploymentRepository.save(deployment);

        } catch (Exception e) {
            log.error("Failed to execute infrastructure start sequence for ID: {}", id, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
            throw new RuntimeException("Infrastructure start action failure", e);
        }
    }

    @Override
    @Transactional
    public void stopDeployment(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment footprint target not found"));

        if (deployment.getContainerName() != null) {
            log.info("Stopping container: {}", deployment.getContainerName());
            dockerService.stopAndRemoveContainer(deployment.getContainerName());
        }

        deployment.setStatus(DeploymentStatus.STOPPED);
        deploymentRepository.save(deployment);
    }

    @Override
    @Transactional
    public void restartDeployment(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment footprint target not found"));

        if (deployment.getContainerName() == null) {
            throw new IllegalStateException("Cannot restart a deployment that has no active container footprint");
        }

        try {
            log.info("Restarting container: {}", deployment.getContainerName());
            ProcessBuilder pb = new ProcessBuilder("docker", "restart", deployment.getContainerName());
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Docker daemon failed to execute restart sequence command context");
            }

            // Verify the runtime is still healthy following the bounce transition
            boolean isHealthy = dockerService.pollHealthCheck(deployment.getApplicationUrl(), 5, 2000);
            if (isHealthy) {
                deployment.setStatus(DeploymentStatus.RUNNING);
            } else {
                deployment.setStatus(DeploymentStatus.FAILED);
            }
            deploymentRepository.save(deployment);

        } catch (Exception e) {
            log.error("Failed executing restart command lifecycle", e);
            throw new RuntimeException("Container runtime restart failure", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getDeploymentLogs(Long deploymentId) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment footprint target not found"));

        if (deployment.getContainerName() == null) {
            return "No execution runtime environment active for this deployment history log constraint.";
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "logs", "--tail", "500", deployment.getContainerName());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder logBuffer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logBuffer.append(line).append("\n");
                }
            }
            process.waitFor();
            return logBuffer.toString();

        } catch (Exception e) {
            log.error("Failed fetching live stream buffer maps from container process block", e);
            return "Infrastructure error occurred while attempting to parse execution logging context loops.";
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DeploymentStatus getDeploymentStatus(Long id) {
        return deploymentRepository.findById(id)
                .map(Deployment::getStatus)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment target not found for ID: " + id));
    }

    private void executeSystemCommand(File workingDir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[DOCKER-BUILD] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Docker compilation process failed with code: " + exitCode);
        }
    }

    private void deleteDirectory(File path) {
        File[] files = path.listFiles();
        if (files != null) {
            for (File f : files) deleteDirectory(f);
        }
        path.delete();
    }
}