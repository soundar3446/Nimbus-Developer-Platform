package com.nimbus.backend.project.repository;

import com.nimbus.backend.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"owner"})
    List<Project> findByOwnerEmail(String email);

    @Query("""
        SELECT p
        FROM Project p
        JOIN FETCH p.owner o
        LEFT JOIN FETCH o.githubIntegration
        WHERE p.uuid = :uuid AND o.email = :email
    """)
    Optional<Project> findByUuidAndOwnerEmail(@Param("uuid") String uuid, @Param("email") String email);

    @Query("""
        SELECT p
        FROM Project p
        JOIN FETCH p.owner o
        LEFT JOIN FETCH o.githubIntegration
        WHERE p.uuid = :uuid
""")
    Optional<Project> findByUuid(@Param("uuid")String uuid);

    boolean existsBySubdomain(String subdomain);
    
    boolean existsByCustomDomain(String customDomain);
}
