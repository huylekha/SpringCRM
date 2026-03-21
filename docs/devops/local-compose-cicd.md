# Local Docker Compose CI/CD Pipeline

## Overview

This document describes the local Docker Compose CI/CD pipeline that provides a "simple but production-like" development workflow using:

- **GitHub Actions** for CI (build, test, docker build)
- **Self-hosted runner** on your dev machine for CD
- **Docker Compose** for local deployment (no Kubernetes/ArgoCD yet)

## Architecture

```
GitHub (code push)
  ↓
GitHub Actions (CI)
  ├── Maven build + unit test (affected modules)
  ├── Frontend npm install + unit test
  └── Docker build (affected services only)
  ↓
Deploy on self-hosted runner
  ├── docker compose up -d --no-deps --force-recreate <services>
  └── Logs persist to ./logs/ via bind mount
```

## Quick Start

### 1. Prerequisites

- Docker Desktop with WSL2 backend (Windows) or Docker Engine (Linux/Mac)
- Git
- Node.js 20+ and npm (for local development)
- Java 21+ and Maven (for local development)

### 2. Initial Setup

1. **Clone and setup environment:**
   ```bash
   git clone <your-repo>
   cd SpringCRM
   cp .env.example .env
   ```

2. **Edit `.env` file with your values:**
   ```bash
   # Required secrets
   JWT_PRIVATE_KEY=your-jwt-private-key
   JWT_PUBLIC_KEY=your-jwt-public-key
   NEXTAUTH_SECRET=your-nextauth-secret
   
   # Optional
   SENTRY_DSN=your-sentry-dsn
   KAFKA_BOOTSTRAP_SERVERS=localhost:9092
   ```

3. **Start the full stack:**
   ```bash
   docker compose up -d
   ```

4. **Verify services:**
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8080/actuator/health
   - Auth Service: http://localhost:8081/actuator/health
   - CRM Service: http://localhost:8082/actuator/health
   - PostgreSQL: localhost:5432
   - Redis: localhost:6379

## GitHub Self-Hosted Runner Setup

### 1. Install GitHub Runner

1. **Go to your GitHub repository → Settings → Actions → Runners**
2. **Click "New self-hosted runner"**
3. **Follow the platform-specific instructions:**

   **Windows (PowerShell as Administrator):**
   ```powershell
   # Create a folder
   mkdir actions-runner; cd actions-runner
   
   # Download the latest runner package
   Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip -OutFile actions-runner-win-x64-2.311.0.zip
   
   # Extract the installer
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/actions-runner-win-x64-2.311.0.zip", "$PWD")
   
   # Configure the runner
   ./config.cmd --url https://github.com/YOUR_USERNAME/SpringCRM --token YOUR_TOKEN
   
   # Install and start the service
   ./svc.sh install
   ./svc.sh start
   ```

   **Linux/Mac:**
   ```bash
   # Create a folder
   mkdir actions-runner && cd actions-runner
   
   # Download the latest runner package
   curl -o actions-runner-linux-x64-2.311.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
   
   # Extract the installer
   tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz
   
   # Configure the runner
   ./config.sh --url https://github.com/YOUR_USERNAME/SpringCRM --token YOUR_TOKEN
   
   # Install and start the service
   sudo ./svc.sh install
   sudo ./svc.sh start
   ```

### 2. Configure GitHub Secrets

Go to **Repository → Settings → Secrets and variables → Actions** and add:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `JWT_PRIVATE_KEY` | JWT private key for auth service | `your-private-key` |
| `JWT_PUBLIC_KEY` | JWT public key for auth service | `your-public-key` |
| `NEXTAUTH_SECRET` | NextAuth secret for frontend | `your-nextauth-secret` |
| `SENTRY_DSN` | Sentry DSN for error tracking | `https://...@sentry.io/...` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers (optional) | `localhost:9092` |

## How the Pipeline Works

### 1. Change Detection

The pipeline automatically detects which services changed:

- `backend/shared-lib/*` → affects all backend services
- `backend/auth-service/*` → affects auth-service only
- `backend/crm-service/*` → affects crm-service only
- `backend/api-gateway/*` → affects api-gateway only
- `frontend/*` → affects frontend only

### 2. Testing

Tests run in parallel for affected services:

- **Backend**: `mvn -B test -pl <module> -am`
- **Frontend**: `npm ci && npm run test:ci`

### 3. Docker Build

Docker images are built only for services that:
- Have changes
- Passed tests

Images are tagged with:
- `<service>:<commit-sha>` (for versioning)
- `<service>:latest` (for convenience)

### 4. Deployment

On `main` or `develop` branches:
1. Create `.env` from GitHub secrets
2. Start infrastructure: `docker compose up -d postgres redis`
3. Deploy changed services: `docker compose up -d --no-deps --force-recreate <services>`
4. Verify health endpoints

## Local Development Commands

### Starting Services

```bash
# Start all services
docker compose up -d

# Start only infrastructure
docker compose up -d postgres redis

# Start specific services
docker compose up -d auth-service crm-service

# Rebuild and start (after code changes)
docker compose up -d --build

# Force recreate containers
docker compose up -d --force-recreate
```

### Debugging Commands

```bash
# Check service status
docker compose ps

# View logs (all services)
docker compose logs

# View logs (specific service)
docker compose logs auth-service

# Follow logs in real-time
docker compose logs -f crm-service

# Check health endpoints
curl http://localhost:8081/actuator/health  # auth-service
curl http://localhost:8082/actuator/health  # crm-service
curl http://localhost:8080/actuator/health  # api-gateway

# Check container resource usage
docker stats

# Execute commands in running containers
docker compose exec auth-service bash
docker compose exec postgres psql -U crm_user -d crm_platform
```

### Log Management

Logs are available in multiple locations:

1. **Container logs**: `docker compose logs <service>`
2. **File logs**: `./logs/<service>.log` (plain text)
3. **JSON logs**: `./logs/<service>-json.log` (structured)

```bash
# View file logs
tail -f ./logs/auth-service.log
tail -f ./logs/crm-service-json.log

# Search logs
grep "ERROR" ./logs/*.log
grep "traceId" ./logs/*-json.log | jq .
```

## Troubleshooting

### Common Issues

1. **Port conflicts**:
   ```bash
   # Check what's using the ports
   netstat -tulpn | grep :8080
   
   # Stop conflicting services
   docker compose down
   ```

2. **Database connection issues**:
   ```bash
   # Check PostgreSQL status
   docker compose logs postgres
   
   # Connect to database
   docker compose exec postgres psql -U crm_user -d crm_platform
   ```

3. **Build failures**:
   ```bash
   # Clean and rebuild
   docker compose down
   docker system prune -f
   docker compose build --no-cache
   docker compose up -d
   ```

4. **Health check failures**:
   ```bash
   # Check service logs
   docker compose logs <service>
   
   # Check if service is responding
   curl -v http://localhost:8081/actuator/health
   
   # Check container resources
   docker stats
   ```

### GitHub Actions Debugging

1. **Runner not picking up jobs**:
   - Check runner status in GitHub UI
   - Restart runner service: `sudo ./svc.sh restart`
   - Check runner logs: `tail -f _diag/Runner_*.log`

2. **Build failures**:
   - Check workflow logs in GitHub Actions tab
   - Verify secrets are set correctly
   - Ensure Docker is running on runner machine

3. **Deployment failures**:
   - Check if `.env` file was created correctly
   - Verify Docker Compose is available
   - Check disk space: `df -h`

## Performance Optimization

### Docker Layer Caching

The Dockerfiles are optimized for layer caching:

1. **Maven dependencies** are downloaded before copying source code
2. **npm dependencies** are installed before copying frontend code
3. **Base images** are cached between builds

### Build Speed Tips

```bash
# Use Docker BuildKit for faster builds
export DOCKER_BUILDKIT=1

# Prune unused images periodically
docker system prune -f

# Use multi-stage build cache
docker build --target build backend/auth-service
```

## Rollback Procedures

### Quick Rollback

1. **Find previous working commit**:
   ```bash
   git log --oneline -10
   ```

2. **Option A: Git rollback** (creates new commit):
   ```bash
   git revert <commit-sha>
   git push
   # Pipeline will auto-deploy the reverted version
   ```

3. **Option B: Manual image rollback**:
   ```bash
   # Update .env with previous IMAGE_TAG
   IMAGE_TAG=<previous-commit-sha>
   
   # Redeploy
   docker compose up -d --force-recreate
   ```

### Emergency Stop

```bash
# Stop all services immediately
docker compose down

# Stop specific service
docker compose stop <service>

# Remove containers (keeps data)
docker compose rm -f <service>
```

## Scaling to Kubernetes

When ready to move to Kubernetes:

1. **Keep the same image tagging**: `<service>:<commit-sha>`
2. **Add container registry**: Push images to Harbor/ECR/GHCR
3. **Create Helm charts** or Kubernetes manifests
4. **Setup ArgoCD** for GitOps deployment
5. **Add observability**: Prometheus, Grafana, Jaeger

The current Docker Compose setup provides a solid foundation for this migration.

## Security Considerations

1. **Secrets Management**:
   - Never commit `.env` file
   - Use GitHub Secrets for sensitive data
   - Rotate JWT keys regularly

2. **Network Security**:
   - Services communicate via internal Docker network
   - Only necessary ports are exposed to host

3. **Container Security**:
   - Non-root user in containers
   - Minimal base images (Alpine)
   - Regular image updates

## Monitoring and Observability

### Health Checks

All services expose health endpoints:
- `/actuator/health` (Spring Boot services)
- `/api/health` (Frontend - needs implementation)

### Metrics

Spring Boot services expose Prometheus metrics at `/actuator/prometheus`.

### Logging

Structured JSON logs are available for log aggregation tools like ELK stack or Grafana Loki.

## Next Steps

1. **Add integration tests** to the pipeline
2. **Implement frontend health endpoint**
3. **Add performance testing** with load testing tools
4. **Setup monitoring dashboards**
5. **Plan Kubernetes migration**