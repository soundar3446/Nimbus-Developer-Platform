package com.nimbus.backend.deployment.service;

public interface DeploymentService {
   void triggerDeploymentPipeline(String projectId);
   void stopDeployment(Long deploymentId);
   void restartDeployment(Long deploymentId);
   String getDeploymentLogs(Long deploymentId);
}
