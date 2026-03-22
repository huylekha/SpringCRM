# GitOps Scripts

This directory contains scripts for GitOps automation and change detection in the SpringCRM monorepo.

## 📁 Scripts Overview

### Change Detection
- `detect-changed-services.sh` / `detect-changed-services.ps1` - Detects which services are affected by changes
- `test-change-detection.sh` / `test-change-detection.ps1` - Tests the change detection logic

### Image Tag Updates
- `update-kustomize-image-tags.sh` / `update-kustomize-image-tags.ps1` - Updates image tags in kustomize overlays
- `gitops-update.sh` / `gitops-update.ps1` - Combined change detection and image tag updates

## 🔍 Change Detection

### Usage

**Linux/Mac:**
```bash
# Detect changes between HEAD~1 and HEAD
./detect-changed-services.sh

# Detect changes between specific refs
./detect-changed-services.sh origin/main HEAD

# Output in different formats
OUTPUT_FORMAT=json ./detect-changed-services.sh
OUTPUT_FORMAT=env ./detect-changed-services.sh
OUTPUT_FORMAT=text ./detect-changed-services.sh  # default
```

**Windows:**
```powershell
# Detect changes between HEAD~1 and HEAD
.\detect-changed-services.ps1

# Detect changes between specific refs
.\detect-changed-services.ps1 -BaseRef "origin/main" -TargetRef "HEAD"

# Output in different formats
.\detect-changed-services.ps1 -OutputFormat "json"
.\detect-changed-services.ps1 -OutputFormat "env"
.\detect-changed-services.ps1 -OutputFormat "text"  # default
```

### Logic

The change detection follows these rules:

1. **Service-specific changes:**
   - `backend/auth-service/*` → affects `auth-service`
   - `backend/crm-service/*` → affects `crm-service`
   - `backend/api-gateway/*` → affects `api-gateway`
   - `frontend/*` → affects `frontend`

2. **Shared library changes:**
   - `backend/shared-lib/*` → affects ALL backend services (`auth-service`, `crm-service`, `api-gateway`)

3. **Non-service changes:**
   - `docs/*` → no services affected
   - `k8s/*` → no services affected (infrastructure changes)
   - `*.md` files → no services affected

### Output Formats

**JSON:**
```json
{
  "changed": {
    "auth-service": true,
    "crm-service": false,
    "api-gateway": false,
    "frontend": false,
    "shared-lib": false
  },
  "affected_services": ["auth-service"],
  "has_changes": true
}
```

**Environment Variables:**
```bash
CHANGED_AUTH=true
CHANGED_CRM=false
CHANGED_GATEWAY=false
CHANGED_FRONTEND=false
CHANGED_SHARED=false
AFFECTED_SERVICES=auth-service
HAS_CHANGES=true
```

**Text (default):**
```
auth-service
```

## 🏷️ Image Tag Updates

### Usage

**Linux/Mac:**
```bash
# Update specific services
./update-kustomize-image-tags.sh dev abc123def auth-service crm-service

# Update all services
./update-kustomize-image-tags.sh staging abc123def

# With custom registry
REGISTRY_URL=my-registry.com ./update-kustomize-image-tags.sh prod abc123def
```

**Windows:**
```powershell
# Update specific services
.\update-kustomize-image-tags.ps1 -Environment "dev" -CommitSha "abc123def" -Services @("auth-service", "crm-service")

# Update all services
.\update-kustomize-image-tags.ps1 -Environment "staging" -CommitSha "abc123def"

# With custom registry
.\update-kustomize-image-tags.ps1 -Environment "prod" -CommitSha "abc123def" -RegistryUrl "my-registry.com"
```

### What it does

1. **Validates inputs:** environment, commit SHA format
2. **Updates kustomization.yaml:** Uses `kustomize edit set image` to update image tags
3. **Commits changes:** Creates a commit with the updated tags
4. **Outputs results:** Returns commit SHA and updated services

### Requirements

- `kustomize` command must be installed
- Git repository must be initialized
- Target overlay directory must exist

## 🚀 Combined GitOps Update

### Usage

**Linux/Mac:**
```bash
# Detect changes and update tags
./gitops-update.sh dev abc123def

# With custom base reference
./gitops-update.sh staging abc123def origin/main
```

**Windows:**
```powershell
# Detect changes and update tags
.\gitops-update.ps1 -Environment "dev" -CommitSha "abc123def"

# With custom base reference
.\gitops-update.ps1 -Environment "staging" -CommitSha "abc123def" -BaseRef "origin/main"
```

### Workflow

1. **Detect Changes:** Runs change detection between base ref and target commit
2. **Skip if No Changes:** Exits early if no services are affected
3. **Update Tags:** Updates image tags for only the affected services
4. **Commit:** Creates a GitOps commit with the changes

## 🧪 Testing

### Run Tests

**Linux/Mac:**
```bash
# Test change detection logic
./test-change-detection.sh
```

**Windows:**
```powershell
# Test change detection logic
.\test-change-detection.ps1

# Verbose output
.\test-change-detection.ps1 -Verbose
```

### Test Cases

The tests validate:
- Single service changes
- Multiple service changes
- Shared library changes (affects all backend)
- Documentation changes (no services affected)
- K8s manifest changes (no services affected)
- Mixed changes including shared-lib

## 🔧 Integration with CI/CD

### GitHub Actions Integration

```yaml
- name: Detect changed services
  id: changes
  run: |
    OUTPUT=$(./scripts/detect-changed-services.sh)
    echo "affected_services=$OUTPUT" >> $GITHUB_OUTPUT

- name: Update GitOps manifests
  if: steps.changes.outputs.affected_services != ''
  run: |
    ./scripts/gitops-update.sh ${{ env.ENVIRONMENT }} ${{ github.sha }}
```

### Environment Mapping

- `develop` branch → `dev` environment
- `release/*` branches → `staging` environment
- `main` branch → `prod` environment

## 📝 Configuration

### Environment Variables

- `REGISTRY_URL` - Docker registry URL (default: `localhost:5000`)
- `OUTPUT_FORMAT` - Change detection output format (`json`, `env`, `text`)

### Git Configuration

The scripts will auto-configure git user for commits:
```bash
git config user.name "GitOps Bot"
git config user.email "gitops-bot@springcrm.local"
```

## 🔗 Related Files

- [Kustomize Overlays](../k8s/overlays/) - Target directories for image tag updates
- [GitHub Actions Workflow](../.github/workflows/k8s-gitops-ci-cd.yml) - CI/CD integration
- [ArgoCD Applications](../infra/argocd/applications/) - GitOps applications that sync the changes