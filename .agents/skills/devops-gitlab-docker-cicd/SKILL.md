---
name: devops-gitlab-docker-cicd
description: Designs and implements GitLab CI/CD pipelines with Docker and Docker Compose for Spring Boot and Next.js applications. Use when creating .gitlab-ci.yml, Dockerfiles, deploy scripts, CI variables, or release workflows.
---

# DevOps GitLab Docker CI/CD

## Apply When
- Setting up CI/CD for fullstack projects.
- Creating or updating `.gitlab-ci.yml`.
- Creating Dockerfiles, Compose files, and deploy scripts.
- Hardening deployment flow for Linux Docker hosts.

## Standard Pipeline Flow
1. install
2. build
3. test
4. docker
5. deploy

## Stack Defaults
- Backend: Java 21, Spring Boot, Maven.
- Frontend: Next.js, Node 20.
- Containers: Docker + Docker Compose.
- Deploy target: Linux server with Docker host.

## Generation Rules
- Separate backend and frontend jobs.
- Use GitLab CI variables for all secrets.
- Use `docker:dind` for image build jobs.
- Deploy only from `main` branch.
- Keep deploy scripts idempotent.

## Required Artifacts
- `.gitlab-ci.yml`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `docker/docker-compose.yml`
- `scripts/deploy.sh`

## Operational Improvements (Recommended)
- Add health checks.
- Add rollback strategy.
- Add monitoring integration (Prometheus/Grafana/Sentry as applicable).
- Add blue-green or canary rollout for high-availability environments.
