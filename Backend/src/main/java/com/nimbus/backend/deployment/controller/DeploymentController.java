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
     * POST /api/deployments/{projectId}/clone
     * Fires off the internal engine CI/CD build deployment thread lifecycle.
     */
    @PostMapping("/{projectId}/clone")
    public ResponseEntity<ApiResponse<String>> initializeDeployment(@PathVariable Long projectId) {
        deploymentService.triggerDeploymentPipeline(projectId);
        return ResponseEntity.ok(new ApiResponse<>(
                true,
                "Deployment runner pipeline engine initialized successfully.",
                "Asynchronous context engine processing active."
        ));
    }
}