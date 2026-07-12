package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.enums.ProjectType;
import com.nimbus.backend.deployment.service.ProjectDetectorService;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class ProjectDetectorServiceImpl implements ProjectDetectorService {

    @Override
    public ProjectType detectType(File workspaceRoot) {
        if (workspaceRoot == null || !workspaceRoot.exists() || !workspaceRoot.isDirectory()) {
            return ProjectType.UNKNOWN;
        }

        // Loop through our defined ProjectType enums and match file footprints
        for (ProjectType type : ProjectType.values()) {
            if (type == ProjectType.UNKNOWN) continue;

            File indicatorFile = new File(workspaceRoot, type.getIndicatorFile());
            if (indicatorFile.exists()) {
                return type;
            }
        }

        return ProjectType.UNKNOWN;
    }
}