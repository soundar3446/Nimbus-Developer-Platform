package com.nimbus.backend.project.service;

import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;


import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(ProjectRequest request);
    List<ProjectResponse> getAllUserProjects();
    ProjectResponse getProjectById(Long id);
    ProjectResponse updateProject(Long id, ProjectRequest request);
    void deleteProject(Long id);
}