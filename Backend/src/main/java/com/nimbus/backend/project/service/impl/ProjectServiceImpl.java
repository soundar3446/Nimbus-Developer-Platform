package com.nimbus.backend.project.service.impl;

import com.nimbus.backend.auth.service.CurrentUserService; 
import com.nimbus.backend.common.exception.AlreadyExistsException;
import com.nimbus.backend.common.exception.ResourceNotFoundException;
import com.nimbus.backend.common.util.DnsVerificationUtil;
import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;
import com.nimbus.backend.project.entity.Project;
import com.nimbus.backend.project.mapper.ProjectMapper;
import com.nimbus.backend.project.repository.ProjectRepository;
import com.nimbus.backend.project.service.ProjectService;
import com.nimbus.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final CurrentUserService currentUserService;
    private final DnsVerificationUtil dnsVerificationUtil;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        if (request.getSubdomain() != null && projectRepository.existsBySubdomain(request.getSubdomain())) {
            throw new AlreadyExistsException("Subdomain is already taken");
        }
        if (request.getCustomDomain() != null && projectRepository.existsByCustomDomain(request.getCustomDomain())) {
            throw new AlreadyExistsException("Custom domain is already mapped to another project");
        }

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

        if (request.getSubdomain() != null && !request.getSubdomain().equals(project.getSubdomain()) && projectRepository.existsBySubdomain(request.getSubdomain())) {
            throw new AlreadyExistsException("Subdomain is already taken");
        }
        if (request.getCustomDomain() != null && !request.getCustomDomain().equals(project.getCustomDomain()) && projectRepository.existsByCustomDomain(request.getCustomDomain())) {
            throw new AlreadyExistsException("Custom domain is already mapped to another project");
        }

        projectMapper.updateProjectFromRequest(request, project);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Override
    @Transactional
    public void deleteProject(String uuid) {
        Project project = getProjectAndValidateOwner(uuid);
        projectRepository.delete(project);
    }

    @Override
    @Transactional
    public ProjectResponse verifyCustomDomain(String uuid) {
        Project project = getProjectAndValidateOwner(uuid);

        if (project.getCustomDomain() == null || project.getCustomDomain().isEmpty()) {
            throw new IllegalArgumentException("No custom domain configured for this project");
        }

        boolean isValid = dnsVerificationUtil.verifyCnameRecord(project.getCustomDomain());
        
        project.setCustomDomainVerified(isValid);
        projectRepository.save(project);

        if (!isValid) {
            throw new IllegalArgumentException("DNS verification failed. Please ensure the CNAME record points to cname.nimbus.app and DNS changes have propagated.");
        }

        return projectMapper.toResponse(project);
    }

    private Project getProjectAndValidateOwner(String uuid) {
        String currentEmail = currentUserService.getCurrentUserEmail();
        
        // Optimize: Push the ownership validation down to the SQL query instead of bringing 
        // the project into JVM memory just to check the owner.
        // Returning a 404 instead of 403 for cross-tenant access prevents ID enumeration attacks.
        return projectRepository.findByUuidAndOwnerEmail(uuid, currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found or you do not have permission to manage it."));
    }
}