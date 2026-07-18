package com.nimbus.backend.deployment.controller;

import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.deployment.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
public class DeploymentController {

    private final DeploymentService deploymentService;

    /**
     * POST /api/deployments/{projectUuid}/clone
     * Fires off the internal engine CI/CD build deployment thread lifecycle.
     */
    @PostMapping("/{projectUuid}/clone")
    public ResponseEntity<ApiResponse<String>> initializeDeployment(
            @PathVariable("projectUuid") String projectUuid) {

        deploymentService.triggerDeploymentPipeline(projectUuid);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Deployment runner pipeline engine initialized successfully.",
                "Asynchronous context engine processing active."
        ));
    }

    /**
     * POST /api/deployments/{id}/start
     * Re-spins up a stopped container using its existing compiled image layout.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<Void>> startDeployment(@PathVariable("id") Long id) {
        deploymentService.startDeployment(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Application runtime environment started successfully.",
                null
        ));
    }

    /**
     * POST /api/deployments/{id}/stop
     * Gracefully stops the running container and frees up system host ports.
     */
    @PostMapping("/{id}/stop")
    public ResponseEntity<ApiResponse<Void>> stopDeployment(@PathVariable("id") Long id) {
        deploymentService.stopDeployment(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Application infrastructure runtime stopped successfully.",
                null
        ));
    }

    /**
     * POST /api/deployments/{id}/restart
     * Triggers a fast restart sequence on the active context container.
     */
    @PostMapping("/{id}/restart")
    public ResponseEntity<ApiResponse<Void>> restartDeployment(@PathVariable("id") Long id) {
        deploymentService.restartDeployment(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Application runtime restarted successfully.",
                null
        ));
    }

    /**
     * GET /api/deployments/{id}/logs
     * Fetches live standard output console logs from the underlying Docker process engine.
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<String>> getDeploymentLogs(@PathVariable("id") Long id) {
        String logs = deploymentService.getDeploymentLogs(id);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Deployment container execution runtime logs retrieved successfully.",
                logs
        ));
    }

        /**
         * GET /api/deployments/{id}/status
         * Returns the exact tracking state metrics footprint of the targeting configuration ID.
         */
        @GetMapping("/{id}/status")
        public ResponseEntity<ApiResponse<DeploymentStatus>> getDeploymentStatus(@PathVariable("id") Long id) {
            DeploymentStatus status = deploymentService.getDeploymentStatus(id);
            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Deployment status footprint retrieved successfully.",
                    status
            ));
        }

    /**
     * GET /api/deployments/project/{projectUuid}
     * Returns the complete chronological deployment history matrix tracking records for the target project.
     */
    @GetMapping("/project/{projectUuid}")
    public ResponseEntity<ApiResponse<List<Deployment>>> getProjectHistory(@PathVariable("projectUuid") String projectUuid) {
        List<Deployment> historyTree = deploymentService.getProjectDeploymentHistory(projectUuid);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Project deployment timeline audit history logs retrieved successfully.",
                historyTree
        ));
    }
}