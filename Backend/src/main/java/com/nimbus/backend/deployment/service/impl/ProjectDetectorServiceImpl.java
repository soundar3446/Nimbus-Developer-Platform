//package com.nimbus.backend.deployment.service.impl;
//
//import com.nimbus.backend.deployment.enums.ProjectType;
//import com.nimbus.backend.deployment.service.ProjectDetectorService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.util.Arrays;
//
//@Slf4j
//@Service
//public class ProjectDetectorServiceImpl implements ProjectDetectorService {
//
//    @Override
//    public ProjectType detectType(File workspaceRoot) {
//        if (workspaceRoot == null || !workspaceRoot.exists() || !workspaceRoot.isDirectory()) {
//            return ProjectType.UNKNOWN;
//        }
//
//        String[] rootFiles = workspaceRoot.list();
//        log.info("Root directory contents found: {}", Arrays.toString(rootFiles));
//
//        // Search recursively up to 3 levels deep
//        return searchForIndicator(workspaceRoot, 0, 3);
//    }
//    private ProjectType searchForIndicator(File directory, int currentDepth, int maxDepth) {
//        if (currentDepth > maxDepth || directory == null || !directory.exists()) {
//            return ProjectType.UNKNOWN;
//        }
//
//        File[] files = directory.listFiles();
//        if (files == null) {
//            return ProjectType.UNKNOWN;
//        }
//
//        // 1. Scan the current directory level completely first
//        for (ProjectType type : ProjectType.values()) {
//            if (type == ProjectType.UNKNOWN) continue;
//
//            File indicatorFile = new File(directory, type.getIndicatorFile());
//            if (indicatorFile.exists() && indicatorFile.isFile()) {
//                log.info("Found indicator file '{}' at depth {} in directory: {}",
//                        type.getIndicatorFile(), currentDepth, directory.getName());
//                return type;
//            }
//        }
//
//        // 2. If not found at this level, recurse into subdirectories (ignoring heavy dotfolders like .git or node_modules)
//        for (File file : files) {
//            if (file.isDirectory() && !file.getName().startsWith(".") && !file.getName().equals("node_modules")) {
//                ProjectType foundType = searchForIndicator(file, currentDepth + 1, maxDepth);
//                if (foundType != ProjectType.UNKNOWN) {
//                    return foundType;
//                }
//            }
//        }
//
//        return ProjectType.UNKNOWN;
//    }
//}