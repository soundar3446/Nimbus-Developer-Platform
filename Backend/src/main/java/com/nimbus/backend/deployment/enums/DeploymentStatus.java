package com.nimbus.backend.deployment.enums;

public enum DeploymentStatus {
    QUEUED,
    CLONING,
    CLONE_FAILED,
    DETECTING,
    DETECTION_FAILED,
    GENERATING_DOCKERFILE,
    BUILDING,
    PUSHING,
    DEPLOYING,
    SUCCESSFUL,
    FAILED
}