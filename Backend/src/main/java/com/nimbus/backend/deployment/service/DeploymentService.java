package com.nimbus.backend.deployment.service;

import com.nimbus.backend.deployment.dto.DeploymentResponseDto;
import com.nimbus.backend.deployment.dto.DeploymentTaskEvent;
import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;

import java.util.List;

public interface DeploymentService {
   Deployment initiateDeploymentPipeline(String projectId);
   void triggerDeploymentPipeline(String projectId);
   void startDeployment(Long id);
   void stopDeployment(Long deploymentId);
   void restartDeployment(Long deploymentId);
   String getDeploymentLogs(Long deploymentId);
   DeploymentStatus getDeploymentStatus(Long id);
   List<DeploymentResponseDto> getProjectDeploymentHistory(String projectUuid);
   DeploymentResponseDto rollbackDeployment(Long deploymentId);
   void executeClusterWorkload(DeploymentTaskEvent event) throws Exception;
   void pushImageToRegistry(Long deploymentId, String projectUuid, String registryUrl, String username, String token, String targetImage) throws Exception;
}
