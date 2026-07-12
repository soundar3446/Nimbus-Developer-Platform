package com.nimbus.backend.project.service;

import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(ProjectRequest request, UserDetails userDetails);
    List<ProjectResponse> getAllUserProjects(UserDetails userDetails);
    ProjectResponse getProjectById(Long id, UserDetails userDetails);
    ProjectResponse updateProject(Long id, ProjectRequest request, UserDetails userDetails);
    void deleteProject(Long id, UserDetails userDetails);
}