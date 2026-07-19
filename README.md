# Nimbus

Nimbus is a Kubernetes-native developer platform that automates the application deployment lifecycle from source repository to a running workload inside a Kubernetes cluster.

The platform provides an API-driven deployment engine capable of building container images, deploying applications, managing Kubernetes resources, tracking deployment history, performing automated rollbacks, and securely managing runtime configuration.

---

## Features

### Source Control Integration

- GitHub repository integration
- Repository binding
- Branch selection
- Git metadata tracking

### Container Build Pipeline

- Docker image build
- Configurable Dockerfile path
- Configurable build context
- Local image management

### Kubernetes Deployment

- Kubernetes Deployment creation
- ClusterIP Service creation
- Ingress creation
- Deployment readiness monitoring
- Pod status tracking
- Live application logs
- Runtime status monitoring

### Deployment Management

- Start deployments
- Stop deployments
- Restart deployments
- Rollback deployments
- Deployment history
- Deployment audit trail

### Security

- Kubernetes Secrets
- Runtime environment variables
- Secret-based configuration injection
- Secure application configuration

### Reliability

- Rolling deployments
- Deployment health verification
- Automated rollback
- Deployment timeout handling
- Asynchronous deployment execution

---

# Architecture

```
                 GitHub Repository
                        │
                        ▼
                Repository Binding
                        │
                        ▼
                 Docker Image Build
                        │
                        ▼
                Local Docker Registry
                        │
                        ▼
             Kubernetes Deployment
                        │
         ┌──────────────┼──────────────┐
         ▼              ▼              ▼
     Deployment      Service        Ingress
         │
         ▼
      Running Pods
         │
         ▼
Deployment Status / Logs / History
```

---

# Technology Stack

## Backend

- Java 25
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

## Database

- PostgreSQL

## Containerization

- Docker
- Docker Compose

## Orchestration

- Kubernetes
- Kubernetes Java SDK

## Authentication

- JWT Authentication

---

# Deployment Workflow

```
User
 │
 ▼
Create Project
 │
 ▼
Connect GitHub Repository
 │
 ▼
Configure Dockerfile
 │
 ▼
Build Docker Image
 │
 ▼
Deploy to Kubernetes
 │
 ▼
Create Service
 │
 ▼
Create Ingress
 │
 ▼
Wait Until Ready
 │
 ▼
Application Running
```

---

# Deployment Lifecycle

```
Image Build
     │
     ▼
Deployment Created
     │
     ▼
Pods Scheduled
     │
     ▼
Health Check
     │
     ├──────────────► Running
     │
     ▼
Deployment Failed
     │
     ▼
Automatic Rollback
```

---

# Project Structure

```
src
├── auth
├── common
├── config
├── deployment
├── github
├── project
├── security
├── user
└── util
```

---

# Deployment APIs

## Initialize Deployment

```
POST /api/deployments/{projectUuid}/clone
```

Starts an asynchronous deployment pipeline.

---

## Deployment Status

```
GET /api/deployments/{id}/status
```

Returns the current deployment state.

---

## Deployment Logs

```
GET /api/deployments/{id}/logs
```

Returns application logs from Kubernetes Pods.

---

## Start Deployment

```
POST /api/deployments/{id}/start
```

Starts a stopped deployment.

---

## Stop Deployment

```
POST /api/deployments/{id}/stop
```

Stops a running deployment.

---

## Restart Deployment

```
POST /api/deployments/{id}/restart
```

Restarts an active deployment.

---

## Rollback Deployment

```
POST /api/deployments/{id}/rollback
```

Restores a previous deployment version.

---

## Deployment History

```
GET /api/deployments/project/{projectUuid}
```

Returns the deployment history for a project.

---

# Deployment Tracking

Nimbus records every deployment including:

- Deployment ID
- Project
- Image Tag
- Git Commit SHA
- Branch
- Deployment Status
- Start Time
- Completion Time
- Duration
- Container Name
- Application URL

---

# Security

Nimbus securely manages runtime configuration using Kubernetes Secrets.

Sensitive values are never stored directly inside Deployment manifests.

Environment variables are injected into Pods using Kubernetes Secret references.

---

# Current Capabilities

- GitHub repository integration
- Docker image build
- Kubernetes Deployment creation
- Kubernetes Service creation
- Kubernetes Ingress creation
- Deployment readiness monitoring
- Deployment history
- Rolling deployments
- Automated rollback
- Runtime environment variables
- Kubernetes Secrets
- Live deployment logs
- Deployment status tracking

---

# Future Roadmap

- GitHub Webhooks
- Continuous Deployment
- Build Pipeline Execution
- Multi-stage Build Support
- Image Registry Integration
- Horizontal Pod Autoscaling
- Resource Limits
- Canary Deployments
- Blue-Green Deployments
- Preview Environments
- Multi-cluster Deployments
- Metrics and Observability
- Custom Domains
- HTTPS Certificate Management
- Team and Organization Support
- Role-Based Access Control
