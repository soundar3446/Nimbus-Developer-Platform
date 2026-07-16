package com.nimbus.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectRequest {

    @NotBlank(message = "Project name cannot be blank")
    @Size(max = 100, message = "Project name cannot exceed 100 characters")
    private String name;

    private String description;

    private String githubRepo;

    @Builder.Default
    private String defaultBranch = "main";

    @NotBlank(message = "DockerFile path is empty.")
    private String dockerfilePath;

    @NotBlank(message = "Build context path is empty.")
    private String contextPath;

    @NotBlank(message = "Image name must not be empty.")
    private String imageName;

}