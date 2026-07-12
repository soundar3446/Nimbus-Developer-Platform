package com.nimbus.backend.project.service.impl;

import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.mapper.ProjectMapper;
import com.nimbus.backend.project.repository.ProjectRepository;
import com.nimbus.backend.project.service.ProjectService;
import com.nimbus.backend.user.entity.User;
import com.nimbus.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request, UserDetails userDetails) {
        User owner = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Project project = projectMapper.toEntity(request);
        project.setOwner(owner);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllUserProjects(UserDetails userDetails) {
        return projectRepository.findByOwnerEmail(userDetails.getUsername())
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id, UserDetails userDetails) {
        Project project = getProjectAndValidateOwner(id, userDetails);
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request, UserDetails userDetails) {
        Project project = getProjectAndValidateOwner(id, userDetails);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setGithubRepo(request.getGithubRepo());
        project.setDefaultBranch(request.getDefaultBranch());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(Long id, UserDetails userDetails) {
        Project project = getProjectAndValidateOwner(id, userDetails);
        projectRepository.delete(project);
    }

    // Helper method to enforce ownership validation boundaries
    private Project getProjectAndValidateOwner(Long id, UserDetails userDetails) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        if (!project.getOwner().getEmail().equals(userDetails.getUsername())) {
            throw new AccessDeniedException("You do not have permission to manage this project.");
        }
        return project;
    }
}