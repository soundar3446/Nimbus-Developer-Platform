package com.nimbus.backend.deployment.entity;

import com.nimbus.backend.common.entity.BaseEntity;
import com.nimbus.backend.deployment.enums.DeploymentStatus;
import com.nimbus.backend.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "deployments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeploymentStatus status;

    @Column(name = "git_commit_hash")
    private String gitCommitHash;

    @Column(name = "container_id", length = 64)
    private String containerId;

    @Column(name = "container_name", length = 255)
    private String containerName;

    @Column(name = "host_port")
    private Integer hostPort;

    @Column(name = "application_url", length = 512)
    private String applicationUrl;

    @Column(name = "image_tag")
    private String imageTag;

    @Column(name = "duration_ms")
    private Long durationMs;

}