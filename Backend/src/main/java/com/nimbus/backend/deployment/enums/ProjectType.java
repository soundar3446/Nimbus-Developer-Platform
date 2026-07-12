package com.nimbus.backend.deployment.enums;

public enum ProjectType {
    SPRING_BOOT("pom.xml"),
    REACT_NODE("package.json"),
    PYTHON("requirements.txt"),
    GO("go.mod"),
    RUST("Cargo.toml"),
    UNKNOWN("unknown");

    private final String indicatorFile;

    ProjectType(String indicatorFile) {
        this.indicatorFile = indicatorFile;
    }

    public String getIndicatorFile() {
        return indicatorFile;
    }
}