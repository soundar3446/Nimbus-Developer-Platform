package com.nimbus.backend.project.entity;

import com.nimbus.backend.common.entity.BaseEntity;
import com.nimbus.backend.project.enums.ProjectStatus;
import com.nimbus.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "github_repo")
    private String githubRepo;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    @Column(name = "dockerfile_path", nullable = false, length = 512)
    private String dockerfilePath;

    @Column(name = "context_path", nullable = false, length = 512)
    private String contextPath;

    @Column(name = "image_name", length = 255)
    private String imageName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "environment_variables", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> environmentVariables = new HashMap<>();

    @Column(name = "registry_url")
    private String registryUrl;

    @Column(name = "registry_username")
    private String registryUsername;

    @Column(name = "registry_token")
    private String registryToken;

    @Column(name = "subdomain", unique = true)
    private String subdomain; // e.g., "my-awesome-app" -> my-awesome-app.nimbus.app

    @Column(name = "custom_domain", unique = true)
    private String customDomain; // e.g., "api.company.com"

    @Column(name = "custom_domain_verified")
    private Boolean customDomainVerified = false;

    @PrePersist
    protected void onCreate() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        if (this.defaultBranch == null) {
            this.defaultBranch = "main";
        }
        if (this.status == null) {
            this.status = ProjectStatus.CREATED;
        }

    }
}