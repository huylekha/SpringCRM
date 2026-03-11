# CI/CD Pipeline Design

## 1. Pipeline Goal

Deliver reliable, repeatable releases with strict quality and security gates:

1. build
2. test
3. docker build
4. docker push
5. deploy

## 2. Primary CI Platform

- Primary: GitLab CI (aligned with existing `.gitlab-ci.yml` include strategy).
- Optional parity: GitHub Actions workflow for organizations using GitHub-first runtime.

## 3. Stage Architecture

| Stage | Objective | Typical Jobs |
|---|---|---|
| `build` | compile packages | backend build, frontend build |
| `test` | quality verification | unit tests, integration tests, lint |
| `docker` | package containers | image build, image scan |
| `push` | publish immutable artifacts | push to container registry |
| `deploy` | release to environment | dev/staging/prod deployment |

## 4. GitLab CI Reference

```yaml
stages:
  - build
  - test
  - docker
  - push
  - deploy

variables:
  DOCKER_TLS_CERTDIR: ""
  IMAGE_TAG: $CI_COMMIT_SHA

build_backend:
  stage: build
  image: maven:3.9-eclipse-temurin-21
  script:
    - cd backend
    - mvn -B -DskipTests clean package

build_frontend:
  stage: build
  image: node:20-alpine
  script:
    - cd frontend
    - npm ci
    - npm run build

test_backend:
  stage: test
  image: maven:3.9-eclipse-temurin-21
  script:
    - cd backend
    - mvn -B test

test_frontend:
  stage: test
  image: node:20-alpine
  script:
    - cd frontend
    - npm ci
    - npm run lint
    - npm run test

docker_build:
  stage: docker
  image: docker:27
  services:
    - docker:27-dind
  script:
    - docker build -t $CI_REGISTRY_IMAGE/auth-service:$IMAGE_TAG backend/auth-service
    - docker build -t $CI_REGISTRY_IMAGE/crm-service:$IMAGE_TAG backend/crm-service
    - docker build -t $CI_REGISTRY_IMAGE/api-gateway:$IMAGE_TAG backend/api-gateway
    - docker build -t $CI_REGISTRY_IMAGE/frontend:$IMAGE_TAG frontend

docker_push:
  stage: push
  image: docker:27
  services:
    - docker:27-dind
  script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
    - docker push $CI_REGISTRY_IMAGE/auth-service:$IMAGE_TAG
    - docker push $CI_REGISTRY_IMAGE/crm-service:$IMAGE_TAG
    - docker push $CI_REGISTRY_IMAGE/api-gateway:$IMAGE_TAG
    - docker push $CI_REGISTRY_IMAGE/frontend:$IMAGE_TAG

deploy_staging:
  stage: deploy
  only:
    - develop
  script:
    - ./devops/scripts/deploy.sh staging $IMAGE_TAG

deploy_prod:
  stage: deploy
  only:
    - main
  when: manual
  script:
    - ./devops/scripts/deploy.sh production $IMAGE_TAG
```

## 5. Quality Gates

Mandatory pass conditions before deploy:

- Backend unit and integration tests pass.
- Frontend lint and tests pass.
- Container image scan passes critical threshold policy.
- Contract checks for API compatibility (recommended for later phase).

## 6. Environment Promotion Policy

- `feature/*` -> CI build + test only.
- `develop` -> deploy to staging automatically.
- `main` -> production deploy via manual approval gate.
- Tag releases (`vX.Y.Z`) for production auditability.

## 7. Secrets and Configuration

Required CI variables:

- `CI_REGISTRY_USER`, `CI_REGISTRY_PASSWORD`
- `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY`
- `DB_PASSWORD`
- `REDIS_PASSWORD` (if enabled)
- `SENTRY_DSN`
- `SSH_PRIVATE_KEY` (if SSH deployment model is used)

Secrets must be masked and protected in CI settings.

## 8. Deployment Strategies

- Staging: rolling deployment is sufficient for first phase.
- Production:
  - preferred blue/green or canary if infrastructure allows
  - otherwise controlled rolling with health checks and rollback script.

## 9. Rollback Plan

- Rollback trigger:
  - elevated error rate, failed health checks, SLA breach.
- Rollback method:
  - redeploy previous image tag for impacted service(s).
- Post-rollback:
  - incident record with root cause and prevention action.

## 10. Gap Notice for Current Repository

Current root CI file references:

- `cicd/.gitlab-ci.yml`

This file must be created when implementation starts, and should align with the blueprint in this document.
