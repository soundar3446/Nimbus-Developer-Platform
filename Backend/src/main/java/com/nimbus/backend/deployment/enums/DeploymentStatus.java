package com.nimbus.backend.deployment.enums;

public enum DeploymentStatus {
    QUEUED,
    CLONING,
    BUILDING,
    STARTING_CONTAINER,
    HEALTH_CHECK,
    RUNNING,
    SUCCESSFUL,
    FAILED,
    STOPPED
}