package com.nimbus.backend.deployment.service;

import java.io.File;

public interface GitService {
    File cloneRepository(String repoUrl, String accessToken, String branch);
}
