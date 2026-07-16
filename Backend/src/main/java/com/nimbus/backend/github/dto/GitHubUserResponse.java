package com.nimbus.backend.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GitHubUserResponse {
    private String id;
    @JsonProperty("login")
    private String username;
    @JsonProperty("avatar_url")
    private String avatarUrl;
}