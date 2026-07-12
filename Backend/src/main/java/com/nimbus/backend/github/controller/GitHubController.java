package com.nimbus.backend.github.controller;

import com.nimbus.backend.common.dto.ApiResponse;
import com.nimbus.backend.github.dto.GitHubRepoResponse;
import com.nimbus.backend.github.dto.GitHubUrlResponse;
import com.nimbus.backend.github.dto.GitHubUserResponse;
import com.nimbus.backend.github.service.GitHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService gitHubService;

    /**
     * GET /api/gitHub/login
     * Generates and returns the target redirection link pointing to the GitHub Login portal.
     */
    @GetMapping("/login")
    public ResponseEntity<ApiResponse<GitHubUrlResponse>> loginRedirect() {
        GitHubUrlResponse response = gitHubService.generateAuthorizationUrl();
        return ResponseEntity.ok(new ApiResponse<>(true, "Redirect link ready", response));
    }

    /**
     * GET /api/gitHub/callback
     * Consumes OAuth code and state passed as a query variable to verify integration.
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<Void>> callback(@RequestParam("code") String code,
                                                      @RequestParam("state") String state) {
        gitHubService.handleOAuthCallback(code, state);
        return ResponseEntity.ok(new ApiResponse<>(true, "Integration completed successfully", null));
    }

    /**
     * GET /api/gitHub/profile
     * Fetches fresh GitHub user metadata associated with the linked profile.
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<GitHubUserResponse>> getProfile() {
        GitHubUserResponse response = gitHubService.getConnectedProfile();
        return ResponseEntity.ok(new ApiResponse<>(true, "GitHub profile details fetched", response));
    }

    /**
     * GET /api/gitHub/repos
     * Enumerates list of active repositories accessible via target access tokens.
     */
    @GetMapping("/repos")
    public ResponseEntity<ApiResponse<List<GitHubRepoResponse>>> getRepositories() {
        List<GitHubRepoResponse> response = gitHubService.getConnectedRepositories();
        return ResponseEntity.ok(new ApiResponse<>(true, "User repositories mapped", response));
    }

    /**
     * POST /api/gitHub/connect/{projectId}
     * Attaches an external repository identifier to an internal setup tracker record.
     */
    @PostMapping("/connect/{projectId}")
    public ResponseEntity<ApiResponse<Void>> connectProject(
            @PathVariable Long projectId,
            @RequestParam("repoUrl") String repoUrl) {

        gitHubService.connectProjectToRepo(projectId, repoUrl);
        return ResponseEntity.ok(new ApiResponse<>(true, "Project successfully bound to GitHub repository", null));
    }
}