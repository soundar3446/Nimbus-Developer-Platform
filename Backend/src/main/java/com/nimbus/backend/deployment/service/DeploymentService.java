package com.nimbus.backend.deployment.service;

import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;

import java.util.List;

public interface DeploymentService {
   void triggerDeploymentPipeline(String projectId);
   void startDeployment(Long id);
   void stopDeployment(Long deploymentId);
   void restartDeployment(Long deploymentId);
   String getDeploymentLogs(Long deploymentId);
   DeploymentStatus getDeploymentStatus(Long id);
   List<Deployment> getProjectDeploymentHistory(String projectUuid);
   Deployment rollbackDeployment(Long deploymentId);
}
