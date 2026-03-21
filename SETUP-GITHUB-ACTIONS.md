# GitHub Actions CI/CD Setup Guide

## 🎯 **Overview**

This guide will help you setup GitHub Actions CI/CD pipeline for local Docker Compose deployment.

## 📋 **Prerequisites**

- ✅ Docker and Docker Compose installed
- ✅ All services running locally (completed in previous steps)
- ✅ GitHub repository with admin access
- ✅ Windows/Linux/Mac machine for self-hosted runner

## 🔧 **Step 1: Setup Self-Hosted Runner**

### **Why Self-Hosted Runner?**
- Deploy directly to your local machine
- Access to local Docker daemon
- No need for cloud infrastructure
- Perfect for development/staging environments

### **Setup Instructions:**

1. **Go to GitHub Repository Settings:**
   ```
   https://github.com/YOUR_USERNAME/SpringCRM/settings/actions/runners
   ```

2. **Click "New self-hosted runner"**

3. **Choose your OS and follow the commands:**

   **Windows (PowerShell as Administrator):**
   ```powershell
   # Create folder for runner
   mkdir actions-runner; cd actions-runner
   
   # Download latest runner (check GitHub for latest version)
   Invoke-WebRequest -Uri https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip -OutFile actions-runner-win-x64-2.311.0.zip
   
   # Extract
   Add-Type -AssemblyName System.IO.Compression.FileSystem
   [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD/actions-runner-win-x64-2.311.0.zip", "$PWD")
   
   # Configure (replace with your actual URL and token from GitHub)
   ./config.cmd --url https://github.com/YOUR_USERNAME/SpringCRM --token YOUR_RUNNER_TOKEN
   
   # Install as Windows service
   ./svc.sh install
   ./svc.sh start
   ```

   **Linux/Mac:**
   ```bash
   # Create folder for runner
   mkdir actions-runner && cd actions-runner
   
   # Download latest runner
   curl -o actions-runner-linux-x64-2.311.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
   
   # Extract
   tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz
   
   # Configure
   ./config.sh --url https://github.com/YOUR_USERNAME/SpringCRM --token YOUR_RUNNER_TOKEN
   
   # Install as service
   sudo ./svc.sh install
   sudo ./svc.sh start
   ```

4. **Verify runner is online in GitHub Settings**

## 🔐 **Step 2: Configure GitHub Secrets**

Go to: `Repository → Settings → Secrets and variables → Actions`

Click **"New repository secret"** and add each of these:

### **Required Secrets:**

| Secret Name | Value | How to Get |
|-------------|-------|------------|
| `JWT_PRIVATE_KEY` | Your JWT private key | Copy from `.env` file |
| `JWT_PUBLIC_KEY` | Your JWT public key | Copy from `.env` file |
| `NEXTAUTH_SECRET` | NextAuth secret | Copy from `.env` file |

### **Optional Secrets:**

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `SENTRY_DSN` | Sentry DSN URL | For error tracking (optional) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | If using Kafka (optional) |

### **Get Values from .env File:**

```bash
# Show current values
cat .env

# Copy these values to GitHub Secrets:
# JWT_PRIVATE_KEY=your-private-key-here
# JWT_PUBLIC_KEY=your-public-key-here  
# NEXTAUTH_SECRET=your-nextauth-secret-here
```

## 🚀 **Step 3: Test the Pipeline**

1. **Make a small change and commit:**
   ```bash
   # Make a small change
   echo "# CI/CD Test" >> README.md
   
   # Commit and push
   git add .
   git commit -m "test: trigger CI/CD pipeline"
   git push origin main
   ```

2. **Check GitHub Actions tab:**
   ```
   https://github.com/YOUR_USERNAME/SpringCRM/actions
   ```

3. **Monitor the pipeline:**
   - ✅ **detect-changes**: Detects which services changed
   - ✅ **test-backend-***: Runs tests for changed backend services  
   - ✅ **docker-build-***: Builds Docker images for changed services
   - ✅ **deploy-local**: Deploys to your local machine

## 📊 **Pipeline Stages Explained**

### **Stage 1: Change Detection**
```yaml
detect-changes:
  - Analyzes git diff to find changed files
  - Sets outputs: backend-changed, frontend-changed, auth-changed, crm-changed
  - Only builds/tests services that actually changed
```

### **Stage 2: Testing (Parallel)**
```yaml
test-backend-auth:     # Only if auth service changed
test-backend-crm:      # Only if CRM service changed  
test-frontend:         # Only if frontend changed
```

### **Stage 3: Docker Build (Parallel)**
```yaml
docker-build-auth:     # Only if auth service changed
docker-build-crm:      # Only if CRM service changed
docker-build-api-gateway: # Only if gateway changed
docker-build-frontend: # Only if frontend changed
```

### **Stage 4: Deploy**
```yaml
deploy-local:
  - Creates .env from GitHub secrets
  - Runs: docker compose up -d --no-deps --force-recreate
  - Only recreates containers for changed services
```

## 🔍 **Troubleshooting**

### **Runner Issues:**
```bash
# Check runner status
./svc.sh status

# View runner logs
./svc.sh logs

# Restart runner
./svc.sh stop
./svc.sh start
```

### **Pipeline Issues:**
1. **Check GitHub Actions logs** for detailed error messages
2. **Verify secrets** are correctly set
3. **Check runner connectivity** to Docker daemon
4. **Ensure .env file** is created correctly on runner machine

### **Docker Issues:**
```bash
# On runner machine, check Docker
docker --version
docker compose --version

# Check if services are running
docker compose ps

# Check logs
docker compose logs --tail=20
```

## ✅ **Success Checklist**

- [ ] Self-hosted runner online in GitHub
- [ ] All GitHub secrets configured
- [ ] Pipeline runs successfully on push
- [ ] Services deploy and start correctly
- [ ] All health endpoints return 200 OK
- [ ] No errors in GitHub Actions logs

## 🎉 **Next Steps**

Once CI/CD is working:

1. **Add more environments** (staging, production)
2. **Add deployment notifications** (Slack, email)
3. **Add automated testing** (integration tests, E2E tests)
4. **Add security scanning** (dependency check, SAST)
5. **Add performance monitoring** (metrics, alerts)

---

**Need help?** Check the detailed pipeline documentation in `docs/devops/local-compose-cicd.md`