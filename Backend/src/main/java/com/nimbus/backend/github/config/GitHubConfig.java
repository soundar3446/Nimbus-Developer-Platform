package com.nimbus.backend.github.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nimbus.github")
@Data
public class GitHubConfig {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String tokenUrl = "https://github.com/login/oauth/access_token";
    private String userUrl = "https://api.github.com/user";
}