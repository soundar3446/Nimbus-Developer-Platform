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

}