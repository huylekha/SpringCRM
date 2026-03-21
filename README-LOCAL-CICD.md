# Local Docker Compose CI/CD Setup

## Quick Start

1. **Setup environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your JWT keys and secrets
   ```

2. **Start the stack:**
   ```bash
   docker compose up -d
   ```

3. **Verify services:**
   - API Gateway: http://localhost:8080/actuator/health ✅
   - Auth Service: http://localhost:8081/actuator/health ✅
   - CRM Service: http://localhost:8082/actuator/health ✅
   - Frontend: http://localhost:3000 ✅

4. **Test via API Gateway:**
   - Auth Service: http://localhost:8080/api/v1/auth/actuator/health ✅
   - CRM Service: http://localhost:8080/api/v1/crm/actuator/health ✅
   - Frontend Health: http://localhost:3000/api/health ✅

## Current Status

**All Services:** 5/5 Healthy ✅
- PostgreSQL 15.17, Redis 7, Auth Service, CRM Service, API Gateway, Frontend all running

**API Gateway Routing:** Fixed and Working ✅
- Fixed route order priority (specific routes before general routes)
- Fixed StripPrefix configuration conflicts
- All health endpoints accessible via gateway
- Security filters properly configured for public health endpoints

**Frontend:** Fixed and Running ✅
- Fixed husky dependency issue in Docker build
- Fixed TypeScript compilation errors
- Added missing @tanstack/react-query-devtools dependency
- Created public directory for static assets

**Database:** Auto-created via JPA (Flyway temporarily disabled) ✅

## GitHub Actions CI/CD

The repository includes a GitHub Actions workflow that:
- Detects changed services automatically
- Runs tests for affected modules only
- Builds Docker images for changed services
- Deploys to your local dev machine via self-hosted runner

**Setup Instructions:** See [`docs/devops/local-compose-cicd.md`](docs/devops/local-compose-cicd.md)

## Key Features

✅ **Smart change detection** - only builds/tests what changed  
✅ **Optimized Docker builds** - dependency caching for faster builds  
✅ **Health checks** - all services have proper health endpoints  
✅ **Structured logging** - logs available in `./logs/` directory  
✅ **Zero-downtime deploys** - rolling updates with `--no-deps --force-recreate`  
✅ **Easy rollback** - git revert or manual image tag rollback  

## Architecture

```
GitHub Actions (CI)
├── Change Detection
├── Parallel Testing (Maven + npm)
├── Docker Build (affected services)
└── Deploy via Self-hosted Runner
    └── docker compose up -d --no-deps --force-recreate
```

## GitHub Actions CI/CD

### 🚀 **Quick Setup**

1. **Setup Self-Hosted Runner:**
   ```powershell
   # Windows (as Administrator)
   .\scripts\setup-cicd.ps1 -GitHubUrl "https://github.com/YOUR_USERNAME/SpringCRM" -RunnerToken "YOUR_TOKEN"
   
   # Linux/Mac (with sudo)  
   sudo ./scripts/setup-cicd.sh "https://github.com/YOUR_USERNAME/SpringCRM" "YOUR_TOKEN"
   ```

2. **Configure GitHub Secrets:**
   ```powershell
   # Show secrets to copy
   .\scripts\show-secrets.ps1
   ```
   Then add them to: `Repository → Settings → Secrets and variables → Actions`

3. **Test Pipeline:**
   ```bash
   # Make any change and push
   git add . && git commit -m "test: trigger CI/CD" && git push
   ```

### 📋 **Pipeline Features**

- ✅ **Smart Change Detection** - Only builds/tests changed services
- ✅ **Parallel Testing** - Backend and frontend tests run simultaneously  
- ✅ **Docker Caching** - Fast builds with layer caching
- ✅ **Zero-Downtime Deploy** - Only recreates changed containers
- ✅ **Health Verification** - Comprehensive post-deploy health checks
- ✅ **Failure Recovery** - Detailed logs and rollback on failure

**Detailed Setup:** See `SETUP-GITHUB-ACTIONS.md` for complete instructions.

## Next Steps

This setup provides a solid foundation for:
- Migration to Kubernetes + ArgoCD
- Adding container registry (Harbor/ECR/GHCR)
- Implementing GitOps workflows
- Adding observability (Prometheus/Grafana)

**Full Documentation:** [`docs/devops/local-compose-cicd.md`](docs/devops/local-compose-cicd.md)