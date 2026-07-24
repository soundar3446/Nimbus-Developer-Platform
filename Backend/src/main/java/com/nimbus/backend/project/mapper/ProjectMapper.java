package com.nimbus.backend.project.mapper;

import com.nimbus.backend.project.dto.ProjectRequest;
import com.nimbus.backend.project.dto.ProjectResponse;
import com.nimbus.backend.project.entity.Project;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProjectMapper {

    Project toEntity(ProjectRequest request);

    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "owner.email", target = "ownerEmail")
    ProjectResponse toResponse(Project project);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProjectFromRequest(ProjectRequest request, @MappingTarget Project project);
}