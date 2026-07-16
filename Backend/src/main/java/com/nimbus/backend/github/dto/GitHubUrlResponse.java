package com.nimbus.backend.github.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GitHubUrlResponse {
    private String authorizationUrl;
}