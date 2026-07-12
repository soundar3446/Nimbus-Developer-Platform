package com.nimbus.backend.github.service;

import com.nimbus.backend.github.dto.GitHubRepoResponse;
import com.nimbus.backend.github.dto.GitHubUrlResponse;
import com.nimbus.backend.github.dto.GitHubUserResponse;

import java.util.List;

public interface GitHubService {
    GitHubUrlResponse generateAuthorizationUrl();
    void handleOAuthCallback(String code, String state);
    GitHubUserResponse getConnectedProfile();
    List<GitHubRepoResponse> getConnectedRepositories();
    void connectProjectToRepo(Long projectId, String repoUrl);
}