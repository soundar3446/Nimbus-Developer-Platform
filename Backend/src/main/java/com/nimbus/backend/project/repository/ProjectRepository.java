package com.nimbus.backend.project.repository;

import com.nimbus.backend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerEmail(String email);

    Optional<Project> findByUuid(String uuid);
}
