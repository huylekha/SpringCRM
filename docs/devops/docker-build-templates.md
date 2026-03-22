# Docker Build Templates & Best Practices

This document provides Docker build templates and best practices for the SpringCRM monorepo, ensuring correct build context for multi-module Maven builds and optimal Docker layer caching.

## 🏗️ Build Context Strategy

### Multi-Module Maven Backend

For backend services, **always use the `backend/` directory as the build context** to enable multi-module Maven builds:

```bash
# ✅ CORRECT - Use backend/ as context
docker build --context backend/ --file backend/auth-service/Dockerfile .

# ❌ INCORRECT - Using service directory as context
docker build --context backend/auth-service/ --file backend/auth-service/Dockerfile .
```

### Why Backend Context is Required

1. **Maven Multi-Module:** Services depend on `shared-lib` module
2. **Dependency Resolution:** Maven needs access to parent `pom.xml` and all modules
3. **Build Optimization:** Shared dependencies are cached at the backend level

## 📁 Directory Structure

```
backend/
├── pom.xml                    # Parent POM (required for multi-module)
├── shared-lib/               # Shared library module
│   ├── pom.xml
│   └── src/
├── auth-service/             # Auth service module
│   ├── pom.xml
│   ├── Dockerfile           # Service-specific Dockerfile
│   └── src/
├── crm-service/             # CRM service module
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
└── api-gateway/             # API Gateway module
    ├── pom.xml
    ├── Dockerfile
    └── src/
```

## 🐳 Dockerfile Templates

### Backend Service Template

**File:** `backend/auth-service/Dockerfile`

```dockerfile
# Multi-stage build for optimal layer caching
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy parent POM and shared dependencies first (better caching)
COPY pom.xml .
COPY shared-lib/pom.xml shared-lib/
COPY auth-service/pom.xml auth-service/

# Download dependencies (cached layer if POMs don't change)
RUN mvn -B dependency:go-offline -pl shared-lib,auth-service -am

# Copy source code
COPY shared-lib/src shared-lib/src
COPY auth-service/src auth-service/src

# Build the application
RUN mvn -B clean package -pl shared-lib,auth-service -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S app && adduser -S app -G app

# Copy the built JAR
COPY --from=build /app/auth-service/target/*.jar app.jar

# Create logs directory with proper permissions
RUN mkdir -p /logs && chown app:app /logs

# Switch to non-root user
USER app

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Start the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### Frontend Service Template

**File:** `frontend/Dockerfile`

```dockerfile
# Multi-stage build for Next.js
FROM node:20-alpine AS deps
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies (production only for deps stage)
RUN npm ci --only=production --ignore-scripts

# Build stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install all dependencies (including dev dependencies)
RUN npm install --ignore-scripts

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Runtime stage
FROM node:20-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user
RUN addgroup -S app && adduser -S app -G app

# Copy built application
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

# Create logs directory with proper permissions
RUN mkdir -p /logs && chown app:app /logs

# Switch to non-root user
USER app

# Expose port
EXPOSE 3000

# Set environment variables
ENV PORT=3000
ENV NODE_ENV=production

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:3000/api/health || exit 1

# Start the application
CMD ["node", "server.js"]
```

## 🔨 Build Commands

### Local Development

```bash
# Backend services (from repo root)
docker build --context backend --file backend/auth-service/Dockerfile --tag auth-service:dev .
docker build --context backend --file backend/crm-service/Dockerfile --tag crm-service:dev .
docker build --context backend --file backend/api-gateway/Dockerfile --tag api-gateway:dev .

# Frontend (from repo root)
docker build --context frontend --file frontend/Dockerfile --tag frontend:dev .
```

### CI/CD Pipeline

```yaml
# GitHub Actions example
- name: Build Docker image
  run: |
    docker build \
      --context ${{ matrix.context }} \
      --file ${{ matrix.dockerfile }} \
      --tag ${{ env.REGISTRY_URL }}/${{ matrix.service }}:${{ env.IMAGE_TAG }} \
      --tag ${{ env.REGISTRY_URL }}/${{ matrix.service }}:latest \
      --cache-from ${{ env.REGISTRY_URL }}/${{ matrix.service }}:latest \
      .
```

### Docker Compose

```yaml
# docker-compose.yml
version: "3.9"
services:
  auth-service:
    build:
      context: ./backend          # ✅ Correct context
      dockerfile: auth-service/Dockerfile
    image: localhost:5000/auth-service:latest
    
  crm-service:
    build:
      context: ./backend          # ✅ Correct context
      dockerfile: crm-service/Dockerfile
    image: localhost:5000/crm-service:latest
    
  frontend:
    build:
      context: ./frontend         # ✅ Correct context
      dockerfile: Dockerfile
    image: localhost:5000/frontend:latest
```

## ⚡ Optimization Strategies

### 1. Layer Caching

**Order operations by change frequency:**

```dockerfile
# 1. Copy dependency files first (changes rarely)
COPY pom.xml .
COPY shared-lib/pom.xml shared-lib/
COPY auth-service/pom.xml auth-service/

# 2. Download dependencies (cached if POMs unchanged)
RUN mvn dependency:go-offline

# 3. Copy source code last (changes frequently)
COPY shared-lib/src shared-lib/src
COPY auth-service/src auth-service/src
```

### 2. Multi-Stage Builds

**Separate build and runtime environments:**

```dockerfile
# Build stage - includes Maven, source code
FROM maven:3.9-eclipse-temurin-21 AS build
# ... build steps ...

# Runtime stage - only JRE and JAR
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/service/target/*.jar app.jar
```

### 3. BuildKit Features

**Enable BuildKit for advanced features:**

```bash
# Enable BuildKit
export DOCKER_BUILDKIT=1

# Use cache mounts (requires BuildKit)
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline
```

## 🚀 Registry Strategy

### Image Tagging

```bash
# Tag with commit SHA for traceability
docker tag service:latest registry.local/service:abc123def

# Tag with semantic version for releases
docker tag service:latest registry.local/service:v1.2.3

# Tag with environment for clarity
docker tag service:latest registry.local/service:dev-latest
```

### Push Strategy

```bash
# Push multiple tags
docker push registry.local/service:abc123def
docker push registry.local/service:latest
docker push registry.local/service:v1.2.3
```

## 🔍 Troubleshooting

### Common Build Issues

#### 1. Maven Dependencies Not Found

**Problem:** `Could not resolve dependencies for project`

**Solution:** Ensure correct build context and module order:

```dockerfile
# ✅ Build shared-lib first, then service
RUN mvn -B clean package -pl shared-lib,auth-service -am -DskipTests
```

#### 2. File Not Found During COPY

**Problem:** `COPY failed: file not found`

**Solution:** Check build context and file paths:

```bash
# Debug: List files in build context
docker build --context backend --file backend/auth-service/Dockerfile --no-cache .

# Verify paths relative to context
COPY shared-lib/pom.xml shared-lib/  # ✅ Relative to backend/
```

#### 3. Slow Build Times

**Problem:** Builds take too long

**Solutions:**
- Use `.dockerignore` to exclude unnecessary files
- Optimize layer caching order
- Use multi-stage builds
- Enable BuildKit cache mounts

#### 4. Large Image Sizes

**Problem:** Images are too large

**Solutions:**
- Use Alpine base images
- Multi-stage builds (exclude build tools from runtime)
- Remove package managers after installation
- Use distroless images for production

### Debugging Commands

```bash
# Inspect image layers
docker history service:latest

# Check image size breakdown
docker images service:latest

# Run interactive shell in build stage
docker build --target build --tag service:debug .
docker run -it service:debug /bin/bash

# Examine build context
tar -czh . | docker build --file Dockerfile -
```

## 📋 Checklist

### Before Building

- [ ] Verify build context is correct (`backend/` for backend services)
- [ ] Check Dockerfile path relative to context
- [ ] Ensure all required files are in build context
- [ ] Review `.dockerignore` to exclude unnecessary files

### Dockerfile Best Practices

- [ ] Use multi-stage builds
- [ ] Order layers by change frequency
- [ ] Use specific base image tags (not `latest`)
- [ ] Run as non-root user
- [ ] Include health checks
- [ ] Set proper file permissions
- [ ] Use `COPY` instead of `ADD` when possible
- [ ] Minimize number of layers

### Security

- [ ] Scan images for vulnerabilities
- [ ] Use official base images
- [ ] Keep base images updated
- [ ] Don't include secrets in images
- [ ] Use multi-stage to exclude build tools
- [ ] Run as non-root user

### Performance

- [ ] Optimize layer caching
- [ ] Use `.dockerignore`
- [ ] Minimize image size
- [ ] Use BuildKit features
- [ ] Consider distroless images for production

## 🔗 Related Documentation

- [Docker Compose Configuration](./docker-compose-example.md)
- [CI/CD Pipeline](../../.github/workflows/k8s-gitops-ci-cd.yml)
- [Kubernetes Deployment](./k8s-gitops-setup.md)
- [Local Development Setup](./local-compose-cicd.md)