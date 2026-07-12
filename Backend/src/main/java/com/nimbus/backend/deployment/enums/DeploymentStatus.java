package com.nimbus.backend.deployment.enums;

public enum DeploymentStatus {
    QUEUED, CLONING, CLONE_FAILED, DETECTING, DETECTION_FAILED, BUILDING, PUSHING, DEPLOYING, SUCCESSFUL, FAILED
}