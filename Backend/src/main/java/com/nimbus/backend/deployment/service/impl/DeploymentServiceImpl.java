package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.deployment.enums.ProjectType;
import com.nimbus.backend.deployment.repository.DeploymentRepository;
import com.nimbus.backend.deployment.service.DeploymentService;
import com.nimbus.backend.deployment.service.GitService;
import com.nimbus.backend.deployment.service.ProjectDetectorService;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeploymentServiceImpl implements DeploymentService {

    private final ProjectRepository projectRepository;
    private final DeploymentRepository deploymentRepository;
    private final GitService gitService;
    private final ProjectDetectorService detectorService;

    @Override
    @Async("taskExecutor") // 🔥 Runs asynchronously so the REST API returns immediately
    public void triggerDeploymentPipeline(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project target not found"));

        // Initialize tracking record
        Deployment deployment = Deployment.builder()
                .project(project)
                .status(DeploymentStatus.CLONING)
                .build();
        deployment = deploymentRepository.save(deployment);

        File workspace = null;
        try {
            // STEP 1: Git Clone
            String token = project.getOwner().getGithubIntegration().getGithubAccessToken();
            workspace = gitService.cloneRepository(project.getGithubRepo(), token, project.getDefaultBranch());

            // STEP 2: Detect Project Type
            deployment.setStatus(DeploymentStatus.DETECTING);
            deploymentRepository.save(deployment);

            ProjectType detectedType = detectorService.detectType(workspace);
            log.info("Project ID {} auto-detected framework type: {}", projectId, detectedType);

            if (detectedType == ProjectType.UNKNOWN) {
                throw new IllegalArgumentException("Unsupported or unidentifiable project stack footprint configuration.");
            }

            // [Steps 3-6 will build sequentially from here]
            deployment.setStatus(DeploymentStatus.SUCCESSFUL);
            deploymentRepository.save(deployment);

        } catch (Exception e) {
            log.error("Deployment failure on Project ID: {}", projectId, e);
            deployment.setStatus(DeploymentStatus.FAILED);
            deploymentRepository.save(deployment);
        } finally {
            // Housekeeping: delete local workspace directory to protect server storage space
            if (workspace != null && workspace.exists()) {
                deleteDirectory(workspace);
            }
        }
    }

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }
}