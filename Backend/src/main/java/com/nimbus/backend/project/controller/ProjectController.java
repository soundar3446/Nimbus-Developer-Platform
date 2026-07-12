package com.nimbus.backend.project.controller;

import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;
import com.nimbus.backend.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * POST /api/projects
     * Creates a new project for the currently logged-in user.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProjectResponse response = projectService.createProject(request, userDetails);
        ApiResponse<ProjectResponse> apiResponse = new ApiResponse<>(
                true,
                "Project created successfully",
                response
        );
        return new ResponseEntity<>(apiResponse, HttpStatus.CREATED);
    }

    /**
     * GET /api/projects
     * Retrieves all projects belonging to the currently logged-in user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getAllProjects(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ProjectResponse> response = projectService.getAllUserProjects(userDetails);
        ApiResponse<List<ProjectResponse>> apiResponse = new ApiResponse<>(
                true,
                "Projects retrieved successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * GET /api/projects/{id}
     * Retrieves a specific project by its ID (if owned by the calling user).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getProjectById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProjectResponse response = projectService.getProjectById(id, userDetails);
        ApiResponse<ProjectResponse> apiResponse = new ApiResponse<>(
                true,
                "Project retrieved successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * PUT /api/projects/{id}
     * Updates an existing project's metadata by its ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ProjectResponse response = projectService.updateProject(id, request, userDetails);
        ApiResponse<ProjectResponse> apiResponse = new ApiResponse<>(
                true,
                "Project updated successfully",
                response
        );
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * DELETE /api/projects/{id}
     * Hard deletes an existing project resource from the database.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        projectService.deleteProject(id, userDetails);
        ApiResponse<Void> apiResponse = new ApiResponse<>(
                true,
                "Project deleted successfully",
                null
        );
        return ResponseEntity.ok(apiResponse);
    }
}