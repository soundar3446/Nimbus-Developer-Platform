# Nimbus Developer Platform Backend

The backend service for the **Nimbus Developer Platform**, built with **Spring Boot** and **Java 21**. Nimbus is a platform for deploying, managing, and monitoring applications, integrating tightly with GitHub, Docker, Kubernetes, and Prometheus.

## Tech Stack

- **Framework**: Java 21, Spring Boot
- **Database**: PostgreSQL
- **Migrations**: Flyway
- **Security**: Spring Security, OAuth2 (GitHub), JWT
- **Infrastructure & Deployment**: Kubernetes Client Java, Docker
- **Messaging**: Apache Kafka, WebSockets
- **Monitoring**: Prometheus (Metrics collection)
- **Other**: JGit, MapStruct, Lombok

## Features

- **GitHub OAuth2 Authentication**: Secure login flow utilizing GitHub OAuth and JWT tokens.
- **Project & Repository Management**: Clone and manage source code using JGit.
- **CI/CD Pipeline**: Build and deploy applications via Docker and Kubernetes.
- **Real-Time Deployment Tracking**: Stream deployment progress and logs to the frontend via WebSockets and Kafka.
- **Monitoring & Metrics**: Query Prometheus for real-time CPU, Memory, and Network I/O metrics of deployed pods.

## Prerequisites

- Java 21 or higher
- Maven (or use the provided `./mvnw` wrapper)
- Docker & Docker Compose (for local development)
- PostgreSQL
- Apache Kafka
- Kubernetes Cluster (Minikube or Docker Desktop is fine for local dev)

## Getting Started

### 1. Environment Setup

Configure your environment variables. The project expects certain variables to be present (typically managed in `config.env` or your application properties), such as:
- PostgreSQL database credentials.
- GitHub OAuth client ID and secret.
- Kafka broker URLs.
- Prometheus URL.

### 2. Local Infrastructure

Use Docker Compose to spin up the required local infrastructure (PostgreSQL, Kafka, Prometheus, etc.):

```bash
docker-compose up -d
```

### 3. Build and Run

You can build and run the application using the Maven wrapper:

```bash
./mvnw clean install
./mvnw spring-boot:run
```

The server will start by default on `http://localhost:8080`.

## Architecture Overview

1. **Authentication Flow**: 
   `Frontend` -> `GET /api/github/login` -> `Spring Boot` -> `Redirect to GitHub` -> `User Logs In` -> `/api/github/callback` -> `Exchange Code for Access Token & JWT` -> `Store in DB`
2. **Deployment Flow**:
   Initiate Deployment -> Trigger Kafka Event -> Clone Repo -> Build Docker Image -> Deploy to Kubernetes -> Stream progress via WebSockets.
3. **Metrics Flow**:
   Interrogate Prometheus for container CPU, Memory, and Network usage to display in the developer dashboard.