package com.nimbus.backend.project.service.impl;

import com.nimbus.backend.auth.service.CurrentUserService; // 🔥 Inject our new helper component
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.mapper.ProjectMapper;
import com.nimbus.backend.project.repository.ProjectRepository;
import com.nimbus.backend.project.service.ProjectService;
import com.nimbus.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final CurrentUserService currentUserService; // 🔥 Injected via Lombok

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = currentUserService.getCurrentUser(); // 🔥 Resolved directly inside the service business layer

        Project project = projectMapper.toEntity(request);
        project.setOwner(currentUser);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllUserProjects() {
        String email = currentUserService.getCurrentUserEmail();
        return projectRepository.findByOwnerEmail(email)
                .stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(String uuid) {
        Project project = getProjectAndValidateOwner(uuid);
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(String uuid, ProjectRequest request) {
        Project project = getProjectAndValidateOwner(uuid);

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setGithubRepo(request.getGithubRepo());
        project.setDefaultBranch(request.getDefaultBranch());
        project.setDockerfilePath(request.getDockerfilePath());
        project.setContextPath(request.getContextPath());

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(String uuid) {
        Project project = getProjectAndValidateOwner(uuid);
        projectRepository.delete(project);
    }

    private Project getProjectAndValidateOwner(String uuid) {
        Project project = projectRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + uuid));

        String currentEmail = currentUserService.getCurrentUserEmail();
        if (!project.getOwner().getEmail().equals(currentEmail)) {
            throw new AccessDeniedException("You do not have permission to manage this project.");
        }
        return project;
    }
}