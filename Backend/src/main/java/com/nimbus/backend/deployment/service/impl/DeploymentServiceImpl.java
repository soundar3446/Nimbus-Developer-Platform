package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.deployment.dto.DeploymentResponseDto;
import com.nimbus.backend.deployment.dto.DeploymentSummaryDto;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.deployment.mapper.DeploymentMapper;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.deployment.service.*;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.enums.ProjectStatus;
import com.nimbus.backend.project.repository.ProjectRepository;
import io.kubernetes.client.openapi.models.V1Deployment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentServiceImpl implements DeploymentService {

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final GitService gitService;
    private final KubernetesService kubernetesService;
    private final DeploymentTrackingEngine deploymentTrackingEngine;
    private final DeploymentMapper deploymentMapper;

    @Override
    @Async("taskExecutor") // Runs asynchronously so the REST API returns immediately
    public void triggerDeploymentPipeline(String projectId) {

        long startTime = System.currentTimeMillis();
        log.info("Running on thread: {}", Thread.currentThread().getName());

        Project project = projectRepository.findByUuid(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project target not found"));

        String activeBranch = (project.getDefaultBranch() != null && !project.getDefaultBranch().isBlank())
                ? project.getDefaultBranch() : "main";
        String targetImage = project.getImageName() + ":latest";

        // Initialize tracking record
        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.CLONING)
                .imageTag(targetImage)
                .gitCommitHash("Fetching....")
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
            Map<String, String> envVars = project.getEnvironmentVariables();

            log.info("Orchestrating Kubernetes Deployment object: {}", k8sDeploymentName);
            kubernetesService.deployApplication(k8sDeploymentName, targetImage, targetPort, envVars);

            log.info("Orchestrating Kubernetes Service object: {}", k8sDeploymentName);
            kubernetesService.createClusterIPService(k8sDeploymentName, targetPort);

            log.info("Orchestrating Kubernetes Ingress object: {}", k8sDeploymentName);
            kubernetesService.createApplicationIngress(k8sDeploymentName);

            deployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            deployment.setContainerName(k8sDeploymentName);
            deploymentRepository.save(deployment);

            int timeoutSeconds = 180;
            boolean isHealthy = deploymentTrackingEngine.waitUntilDeploymentReady(k8sDeploymentName, timeoutSeconds);

            DeploymentSummaryDto summary = kubernetesService.getDeploymentSummary(k8sDeploymentName);

            deployment.setDurationMs(System.currentTimeMillis() - startTime);

            if (isHealthy) {
                log.info("Pod successfully scheduled and running in cluster! Promoting deployment status.");
                deployment.setStatus(DeploymentStatus.RUNNING);
                project.setStatus(ProjectStatus.CONNECTED);
            } else {
                log.error("Pod context compilation failed to reach a Running state within timeout limits.");
                deployment.setStatus(DeploymentStatus.FAILED);

                String structuralErrorLogs = kubernetesService.fetchLogs(k8sDeploymentName);
                log.error("--- CAPTURED CONTAINER CRASH LOG ENGINE OUTPUT ---\n{}", structuralErrorLogs);
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

        String k8sDeploymentName = deployment.getContainerName();
        String namespace = "default";

        try {
            log.info("Starting existing Kubernetes container layout: {}", k8sDeploymentName);

            boolean needsRecreation = false;
            Optional<V1Deployment> deploymentOpt = Optional.empty();

            deploymentOpt = kubernetesService.getDeployment(k8sDeploymentName);

                if (deploymentOpt.isEmpty()) {
                    log.warn("Kubernetes deployment footprint missing from cluster engine. Re-instantiating fresh container runtime layout.");
                    int targetPort = 8080;
                    String targetImage = deployment.getImageTag();
                    Map<String, String> envVars = deployment.getProject().getEnvironmentVariables();

                    kubernetesService.deployApplication(k8sDeploymentName, targetImage, targetPort, envVars);
                    kubernetesService.createClusterIPService(k8sDeploymentName, targetPort);
                    kubernetesService.createApplicationIngress(k8sDeploymentName);
                } else {
                    log.info("Deployment footprint found. Scaling replica configuration back up to 1.");
                    V1Deployment k8sDep = deploymentOpt.get();

                    kubernetesService.updateDeploymentReplicas(deploymentOpt.get(), 1);
                }

            deployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            deploymentRepository.save(deployment);

            int timeoutSeconds = 180;
            boolean isHealthy = deploymentTrackingEngine.waitUntilDeploymentReady(k8sDeploymentName, timeoutSeconds);

            if (isHealthy) {
                log.info("Application successfully back online inside cluster mesh at route: {}", deployment.getApplicationUrl());
                deployment.setStatus(DeploymentStatus.RUNNING);
            } else {
                log.error("Application failed to spin up healthy pod replicas within tracking timeout limits.");
                deployment.setStatus(DeploymentStatus.FAILED);

                String failureLogs = kubernetesService.fetchLogs(k8sDeploymentName);
                log.error("--- CAPTURED RE-START CLUSTER LOGS ---\n{}", failureLogs);
            }

            deploymentRepository.save(deployment);

        }catch (Exception e) {
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

        String k8sDeploymentName = deployment.getContainerName();

        if (k8sDeploymentName != null) {
            log.info("Stopping Kubernetes application environment target: {}", k8sDeploymentName);

            try {

                java.util.Optional <V1Deployment> deploymentOpt =
                        kubernetesService.getDeployment(k8sDeploymentName);

                if (deploymentOpt.isPresent()) {
                    kubernetesService.updateDeploymentReplicas(deploymentOpt.get(), 0);
                    log.info("Successfully scaled deployment {} down to 0 replicas.", k8sDeploymentName);
                } else {
                    log.warn("Deployment object {} already absent from cluster control plane.", k8sDeploymentName);
                }
            } catch (Exception e) {
                log.error("Failed to cleanly scale down cluster deployment for ID: {}", deploymentId, e);
                throw new RuntimeException("Kubernetes scaling operation failed while stopping application", e);
            }
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

        String k8sDeploymentName = deployment.getContainerName();

        try {
            log.info("Initiating cloud-native cluster rollout restart for: {}", k8sDeploymentName);

            // 1. Safe object extraction lookup
            java.util.Optional<io.kubernetes.client.openapi.models.V1Deployment> deploymentOpt =
                    kubernetesService.getDeployment(k8sDeploymentName);

            if (deploymentOpt.isEmpty()) {
                throw new IllegalStateException("Active deployment blueprint missing from cluster plane. Run Start first.");
            }

            deployment.setStatus(DeploymentStatus.HEALTH_CHECK);
            deploymentRepository.save(deployment);

            // 2. Trigger the annotation modification rollout step
            kubernetesService.rolloutRestartDeployment(deploymentOpt.get());

            // 3. Monitor the live transition using the replica tracking engine
            int timeoutSeconds = 180;
            boolean isHealthy = deploymentTrackingEngine.waitUntilDeploymentReady(k8sDeploymentName, timeoutSeconds);

            if (isHealthy) {
                log.info("Application rolling rollout restart finalized successfully for: {}", k8sDeploymentName);
                deployment.setStatus(DeploymentStatus.RUNNING);
            } else {
                log.error("Application pods failed to stabilize following rollout bounce sequence.");
                deployment.setStatus(DeploymentStatus.FAILED);
            }

            deploymentRepository.save(deployment);

        } catch (Exception e) {
            log.error("Failed executing cloud-native cluster restart command lifecycle for ID: {}", deploymentId, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
            throw new RuntimeException("Kubernetes deployment rollout restart action failure", e);
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

        String k8sDeploymentName = deployment.getContainerName(); // Contains "nimbus-{id}"

        try {
            log.info("Fetching cloud-native runtime pod logs for application: {}", k8sDeploymentName);

            return kubernetesService.fetchLogs(k8sDeploymentName);

        } catch (Exception e) {
            log.error("Failed fetching live stream buffer maps from cluster pod block for deployment ID: {}", deploymentId, e);
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

    @Override
    @Transactional(readOnly = true)
    public List<DeploymentResponseDto> getProjectDeploymentHistory(String projectUuid) {
        Project project = projectRepository.findByUuid(projectUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Project target not found for UUID: " + projectUuid));

        List<Deployment> historyTree = deploymentRepository.findByProjectIdOrderByIdDesc(project.getId());

        return deploymentMapper.toResponseDtoList(historyTree);
    }

    @Override
    @Transactional
    public DeploymentResponseDto rollbackDeployment(Long deploymentId) {
        long startTime = System.currentTimeMillis();

        Deployment historicalTarget = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Historical deployment record not found for ID: " + deploymentId));

        if (historicalTarget.getImageTag() == null) {
            throw new IllegalStateException("Target deployment cannot be used for rollback as it lacks a valid compiled image tag.");
        }

        Project project = historicalTarget.getProject();
        String k8sDeploymentName = "nimbus-" + historicalTarget.getId();

        log.info("Initiating historical rollback recovery sequence to image version: [ {} ]", historicalTarget.getImageTag());

        Deployment rollbackAudit = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.STARTING_CONTAINER)
                .imageTag(historicalTarget.getImageTag())
                .gitCommitHash(historicalTarget.getGitCommitHash() != null ? historicalTarget.getGitCommitHash() : "Rollback Source")
                .containerName(historicalTarget.getContainerName())
                .applicationUrl(historicalTarget.getApplicationUrl())
                .hostPort(historicalTarget.getHostPort())
                .build();
        rollbackAudit = deploymentRepository.save(rollbackAudit);

        try {

            Map<String, String> currentEnvVars = project.getEnvironmentVariables();
            int targetPort = 8080;

            java.util.Optional<io.kubernetes.client.openapi.models.V1Deployment> deploymentOpt =
                    kubernetesService.getDeployment(historicalTarget.getContainerName());

            if (deploymentOpt.isEmpty()) {
                log.warn("Active cluster workload structure missing during rollback. Performing full manifest reconstruction.");
                kubernetesService.deployApplication(historicalTarget.getContainerName(), historicalTarget.getImageTag(), targetPort, currentEnvVars);
                kubernetesService.createClusterIPService(historicalTarget.getContainerName(), targetPort);
                kubernetesService.createApplicationIngress(historicalTarget.getContainerName());
            } else {
                log.info("Active workload footprint found. Updating live pod template image context to target rollback version.");
                // We'll leverage a new helper method in our Kubernetes service layer to update the image tag inline
                kubernetesService.updateDeploymentImage(historicalTarget.getContainerName(), historicalTarget.getImageTag(), currentEnvVars);
            }

            rollbackAudit.setStatus(DeploymentStatus.HEALTH_CHECK);
            deploymentRepository.save(rollbackAudit);

            // 5. Block and poll tracking metrics until cluster replica saturation completes successfully
            int timeoutSeconds = 180;
            boolean isHealthy = deploymentTrackingEngine.waitUntilDeploymentReady(historicalTarget.getContainerName(), timeoutSeconds);

            rollbackAudit.setDurationMs(System.currentTimeMillis() - startTime);

            if (isHealthy) {
                log.info("Rollback recovery finalized successfully. Application version is fully active.");
                rollbackAudit.setStatus(DeploymentStatus.RUNNING);
                project.setStatus(ProjectStatus.CONNECTED);
            } else {
                log.error("Rollback recovery failed to reach stable cluster running limits within threshold windows.");
                rollbackAudit.setStatus(DeploymentStatus.FAILED);
            }

            projectRepository.save(project);
            Deployment savedEntity = deploymentRepository.save(rollbackAudit);

            return deploymentMapper.toResponseDto(savedEntity);

        } catch (Exception e) {
            log.error("Systemic error encountered executing cluster rollback routine for historical target ID: {}", deploymentId, e);
            rollbackAudit.setStatus(DeploymentStatus.FAILED);
            rollbackAudit.setDurationMs(System.currentTimeMillis() - startTime);
            deploymentRepository.save(rollbackAudit);
            throw new RuntimeException("Platform rollback recovery action failure", e);
        }
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