package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.deployment.service.DeploymentService;
import com.nimbus.backend.deployment.service.GitService;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    @Override
    @Async("taskExecutor") // 🔥 Runs asynchronously so the REST API returns immediately
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

            String targetImage = "nimbus-registry.local/apps/" + projectId + ":latest";

            log.info("🐳 Running user-configured docker build...");
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

            deployment.setStatus(DeploymentStatus.SUCCESSFUL);
            deploymentRepository.save(deployment);
            log.info("🎉 User application container successfully compiled: {}", targetImage);

        } catch (Exception e) {
            log.error("💥 Docker engine compilation failed on project UUID: {}", projectId, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
        } finally {
            if (workspace != null && workspace.exists()) {
                deleteDirectory(workspace);
            }
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