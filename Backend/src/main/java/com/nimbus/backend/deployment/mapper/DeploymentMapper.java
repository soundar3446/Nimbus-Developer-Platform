package com.nimbus.backend.deployment.mapper;

import com.nimbus.backend.deployment.dto.DeploymentResponseDto;
import com.nimbus.backend.deployment.entity.Deployment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface DeploymentMapper {

    @Mapping(source = "project.uuid", target = "projectUuid")
    DeploymentResponseDto toResponseDto(Deployment deployment);

    List<DeploymentResponseDto> toResponseDtoList(List<Deployment> deployments);


}
