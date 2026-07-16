package com.nimbus.backend.deployment.repository;

import com.nimbus.backend.deployment.entity.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment , Long> {

}