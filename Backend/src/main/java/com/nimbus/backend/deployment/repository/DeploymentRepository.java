package com.nimbus.backend.deployment.repository;

import com.nimbus.backend.deployment.entity.Deployment;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment , Long> {
    List<Deployment> findByProjectIdOrderByIdDesc(Long projectId);
    Optional<Deployment> findFirstByProjectIdAndStatusOrderByIdDesc(Long projectId, DeploymentStatus status);
}