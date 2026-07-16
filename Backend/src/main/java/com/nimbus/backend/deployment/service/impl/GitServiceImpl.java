package com.nimbus.backend.deployment.service.impl;

import com.nimbus.backend.deployment.service.GitService;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;

@Slf4j
@Service
public class GitServiceImpl implements GitService {

    @Override
    public File cloneRepository(String repoUrl, String accessToken, String branch) {
        try {
            // Create a unique temporary directory for this build workspace execution
            File localWorkspace = Files.createTempDirectory("nimbus-build-").toFile();
            log.info("Cloning {} into temporary workspace: {}", repoUrl, localWorkspace.getAbsolutePath());

            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(localWorkspace)
                    .setBranch(branch != null ? branch : "main")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", accessToken))
                    .call()
                    .close(); // Clean up resource streams immediately

            return localWorkspace;
        } catch (Exception e) {
            log.error("Failed to clone repository: {}", repoUrl, e);
            throw new RuntimeException("Git clone failed: " + e.getMessage(), e);
        }
    }
}