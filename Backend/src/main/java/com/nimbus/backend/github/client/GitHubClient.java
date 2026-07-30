package com.nimbus.backend.github.client;

import com.nimbus.backend.github.config.GitHubConfig;
import com.nimbus.backend.github.dto.GitHubRepoResponse;
import com.nimbus.backend.github.dto.GitHubTokenResponse;
import com.nimbus.backend.github.dto.GitHubUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GitHubClient {

    private final GitHubConfig gitHubConfig;
    private final RestClient restClient = RestClient.builder().build();

    public GitHubTokenResponse exchangeCodeForToken(String code) {
        org.springframework.util.MultiValueMap<String, String> formData = new org.springframework.util.LinkedMultiValueMap<>();
        formData.add("client_id", gitHubConfig.getClientId());
        formData.add("client_secret", gitHubConfig.getClientSecret());
        formData.add("code", code);
        formData.add("redirect_uri", gitHubConfig.getRedirectUri());

        return restClient.post()
                .uri(gitHubConfig.getTokenUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(GitHubTokenResponse.class);
    }

    public GitHubUserResponse fetchUserProfile(String accessToken) {
        return restClient.get()
                .uri(gitHubConfig.getUserUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(GitHubUserResponse.class);
    }

    public List<GitHubRepoResponse> fetchUserRepositories(String accessToken) {
        return restClient.get()
                .uri("https://api.github.com/user/repos?per_page=100&sort=updated")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<List<GitHubRepoResponse>>() {});
    }
}