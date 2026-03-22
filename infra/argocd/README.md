# ArgoCD GitOps Configuration

This directory contains ArgoCD configuration for the SpringCRM application GitOps deployment.

## 📁 Structure

```
infra/argocd/
├── applications/           # ArgoCD Application manifests
│   ├── argocd-app-dev.yaml      # Development environment
│   ├── argocd-app-staging.yaml  # Staging environment  
│   └── argocd-app-prod.yaml     # Production environment
├── projects/              # ArgoCD AppProject manifests
│   └── springcrm-project.yaml   # SpringCRM project definition
├── install-argocd-apps.sh # Installation script (Linux/Mac)
├── install-argocd-apps.ps1# Installation script (Windows)
└── README.md              # This file
```

## 🚀 Quick Setup

### Prerequisites

1. **Kubernetes cluster running** (Docker Desktop, kind, k3d, etc.)
2. **ArgoCD installed** in the cluster
3. **kubectl configured** to access the cluster

### Install ArgoCD (if not already installed)

```bash
# Create ArgoCD namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd
```

### Install SpringCRM Applications

**Linux/Mac:**
```bash
cd infra/argocd
chmod +x install-argocd-apps.sh
./install-argocd-apps.sh
```

**Windows:**
```powershell
cd infra/argocd
.\install-argocd-apps.ps1
```

## 🏗️ Applications

### Development Environment
- **Name:** `springcrm-dev`
- **Namespace:** `dev`
- **Source:** `k8s/overlays/dev`
- **Sync Policy:** Automated (prune + selfHeal)

### Staging Environment
- **Name:** `springcrm-staging`  
- **Namespace:** `staging`
- **Source:** `k8s/overlays/staging`
- **Sync Policy:** Automated (prune + selfHeal)

### Production Environment
- **Name:** `springcrm-prod`
- **Namespace:** `prod` 
- **Source:** `k8s/overlays/prod`
- **Sync Policy:** Automated selfHeal, **manual prune** (for safety)

## 🔐 Security & RBAC

The `springcrm-project.yaml` defines three roles:

- **Admin:** Full access to all SpringCRM applications
- **Developer:** Can sync dev/staging, read-only prod
- **Readonly:** Read-only access to all environments

## 📊 Monitoring & Management

### Access ArgoCD UI

```bash
# Port forward ArgoCD server
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Open browser to https://localhost:8080
```

### Get Admin Password

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
```

### Check Application Status

```bash
# List all applications
kubectl get applications -n argocd

# Get detailed status
kubectl describe application springcrm-dev -n argocd
```

### Manual Sync (if needed)

```bash
# Using ArgoCD CLI
argocd app sync springcrm-dev
argocd app sync springcrm-staging
argocd app sync springcrm-prod

# Using kubectl
kubectl patch application springcrm-dev -n argocd --type merge -p='{"operation":{"sync":{}}}'
```

## 🔄 GitOps Workflow

1. **Code Change:** Developer pushes code to GitHub
2. **CI Pipeline:** GitHub Actions builds and pushes Docker images
3. **Image Update:** CI updates image tags in `k8s/overlays/<env>/kustomization.yaml`
4. **Git Commit:** CI commits the updated manifests back to the repo
5. **ArgoCD Sync:** ArgoCD detects changes and syncs to Kubernetes
6. **Deployment:** Rolling update with zero downtime

## 🛠️ Troubleshooting

### Application Stuck in "Progressing"

```bash
# Check application events
kubectl describe application springcrm-dev -n argocd

# Check pod status in target namespace
kubectl get pods -n dev
kubectl describe pod <pod-name> -n dev
```

### Sync Failures

```bash
# Get sync status
argocd app get springcrm-dev

# Force refresh
argocd app refresh springcrm-dev

# Hard refresh (ignore cache)
argocd app refresh springcrm-dev --hard
```

### Resource Conflicts

```bash
# Check for resource conflicts
kubectl get events -n dev --sort-by='.lastTimestamp'

# Manually delete conflicting resources
kubectl delete <resource-type> <resource-name> -n <namespace>
```

## 📝 Configuration Notes

### Branch Mapping

- `develop` branch → `dev` environment
- `release/*` branches → `staging` environment  
- `main` branch → `prod` environment

### Image Tags

Images are tagged with commit SHA for traceability:
- `localhost:5000/auth-service:abc123def`
- `localhost:5000/crm-service:abc123def`
- etc.

### Secrets Management

**⚠️ Important:** The current setup uses plain-text secrets in overlays. For production, consider:

- **Sealed Secrets:** Encrypt secrets that can be stored in Git
- **External Secrets Operator:** Sync secrets from external systems
- **SOPS:** Encrypt YAML files with age/PGP keys

## 🔗 Related Documentation

- [Kubernetes Setup Guide](../../docs/devops/k8s-gitops-setup.md)
- [CI/CD Pipeline Documentation](../../.github/workflows/k8s-gitops-ci-cd.yml)
- [Kustomize Overlays](../../k8s/overlays/)