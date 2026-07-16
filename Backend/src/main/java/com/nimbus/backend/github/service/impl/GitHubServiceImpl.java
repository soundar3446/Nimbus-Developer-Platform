package com.nimbus.backend.github.service.impl;

import com.nimbus.backend.auth.service.CurrentUserService;
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.github.client.GitHubClient;
import com.nimbus.backend.github.config.GitHubConfig;
import com.nimbus.backend.github.dto.GitHubRepoResponse;
import com.nimbus.backend.github.dto.GitHubTokenResponse;
import com.nimbus.backend.github.dto.GitHubUrlResponse;
import com.nimbus.backend.github.dto.GitHubUserResponse;
import com.nimbus.backend.github.entity.GitHubIntegration;
import com.nimbus.backend.github.repository.GitHubIntegrationRepository;
import com.nimbus.backend.github.service.GitHubService;
import com.nimbus.backend.github.service.OAuthStateService;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.enums.ProjectStatus;
import com.nimbus.backend.project.repository.ProjectRepository;
import com.nimbus.backend.user.entity.User;
import com.nimbus.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final GitHubConfig config;
    private final GitHubClient client;
    private final CurrentUserService currentUserService;
    private final OAuthStateService oauthStateService; // 🔥 Added dependency
    private final GitHubIntegrationRepository integrationRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository; // 🔥 Added dependency

    @Override
    public GitHubUrlResponse generateAuthorizationUrl() {
        // Resolve who is building this url and register a secured random state parameter token
        User currentUser = currentUserService.getCurrentUser();
        String secureState = oauthStateService.generateAndStoreState(currentUser.getId());

        String url = UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", config.getRedirectUri())
                .queryParam("scope", "user,repo")
                .queryParam("state", secureState) // 🔥 State parameter attached to protect redirect handshake
                .toUriString();
        return new GitHubUrlResponse(url);
    }

    @Override
    @Transactional
    public void handleOAuthCallback(String code, String state) {
        // 1. 🔥 Critical State Parameter Anti-CSRF verification step
        Long expectedUserId = oauthStateService.getUserIdAndEvict(state)
                .orElseThrow(() -> new AccessDeniedException("Invalid, expired, or replayed OAuth state token detected."));

        // 2. Resolve target token exchange with GitHub OAuth engine APIs
        GitHubTokenResponse tokenResponse = client.exchangeCodeForToken(code);
        if (tokenResponse == null) throw new IllegalStateException("Failed to retrieve token from GitHub");

        GitHubUserResponse userResponse = client.fetchUserProfile(tokenResponse.getAccessToken());
        if (userResponse == null) throw new IllegalStateException("Failed to retrieve user info from GitHub");

        // 3. Look up the verified user profile resolved out of the secure temporary eviction cache
        User targetUser = userRepository.findById(expectedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user account not found."));

        GitHubIntegration integration = targetUser.getGithubIntegration();

        if (integration == null) {
            integration = new GitHubIntegration();
            integration.setUser(targetUser);
        }

        integration.setGithubId(userResponse.getId());
        integration.setGithubUsername(userResponse.getUsername());
        integration.setGithubAvatar(userResponse.getAvatarUrl());
        integration.setGithubAccessToken(tokenResponse.getAccessToken());

        integrationRepository.save(integration);
    }

    @Override
    @Transactional(readOnly = true)
    public GitHubUserResponse getConnectedProfile() {
        GitHubIntegration integration = getValidIntegration();
        return client.fetchUserProfile(integration.getGithubAccessToken());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GitHubRepoResponse> getConnectedRepositories() {
        GitHubIntegration integration = getValidIntegration();
        return client.fetchUserRepositories(integration.getGithubAccessToken());
    }

    @Override
    @Transactional
    public void connectProjectToRepo(Long projectId, String repoUrl) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        User currentUser = currentUserService.getCurrentUser();
        if (!project.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Unauthorized project modification attempt.");
        }

        project.setGithubRepo(repoUrl);
        project.setStatus(ProjectStatus.CONNECTED);
        projectRepository.save(project);
    }

    private GitHubIntegration getValidIntegration() {
        User currentUser = currentUserService.getCurrentUser();
        GitHubIntegration integration = currentUser.getGithubIntegration();
        if (integration == null) {
            throw new ResourceNotFoundException("No active GitHub integration linked to this account.");
        }
        return integration;
    }
}