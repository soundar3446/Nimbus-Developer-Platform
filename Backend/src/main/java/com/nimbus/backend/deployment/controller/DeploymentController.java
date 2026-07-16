package com.nimbus.backend.deployment.controller;

import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.deployment.service.DeploymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}