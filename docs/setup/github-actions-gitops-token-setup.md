# GitHub Actions GITOPS_TOKEN Setup Guide

This guide explains how to set up a Personal Access Token (PAT) for GitHub Actions to enable GitOps operations, including pushing updated Kustomize manifests back to the repository.

## Why GITOPS_TOKEN is Needed

The GitHub Actions workflows need to:
1. **Checkout code** with write permissions
2. **Push updated Kustomize manifests** back to the repository after Docker builds
3. **Trigger ArgoCD sync** by updating Git-tracked Kubernetes manifests

The default `github.token` has limited permissions and may not work for all GitOps operations, especially when pushing to protected branches or when additional repository permissions are required.

## Current Workflow Usage

The `GITOPS_TOKEN` is used in these workflows:

### 1. Main CI/CD Workflow (`.github/workflows/k8s-gitops-ci-cd.yml`)

```yaml
- name: Checkout code
  uses: actions/checkout@v4
  with:
    token: ${{ secrets.GITOPS_TOKEN || github.token }}
    fetch-depth: 0
```

```yaml
- name: Push GitOps changes
  shell: pwsh
  run: |
    # Configure git
    git config user.name "GitOps Bot"
    git config user.email "gitops-bot@springcrm.local"
    
    # Push the changes
    git push origin HEAD
```

### 2. Rollback Workflow (`.github/workflows/k8s-rollback.yml`)

```yaml
- name: Checkout code
  uses: actions/checkout@v4
  with:
    token: ${{ secrets.GITOPS_TOKEN || github.token }}
    fetch-depth: 0
```

```yaml
- name: Push rollback changes
  shell: pwsh
  run: |
    # Configure git
    git config user.name "GitOps Rollback Bot"
    git config user.email "gitops-rollback-bot@springcrm.local"
    
    # Push the changes
    git push origin HEAD --force-with-lease
```

## Step 1: Create Personal Access Token (PAT)

### Option A: Fine-Grained Personal Access Token (Recommended)

1. **Go to GitHub Settings:**
   - Navigate to https://github.com/settings/tokens
   - Click "Generate new token" → "Generate new token (beta)"

2. **Configure Token Settings:**
   - **Token name**: `SpringCRM GitOps Token`
   - **Expiration**: 90 days (or custom as needed)
   - **Description**: `Token for SpringCRM GitOps CI/CD workflows`

3. **Repository Access:**
   - **Selected repositories**: Choose your SpringCRM repository
   - **Repository permissions**:
     - **Contents**: Read and write
     - **Metadata**: Read
     - **Pull requests**: Read (if workflows run on PRs)
     - **Actions**: Read (to access workflow context)

4. **Account Permissions:**
   - No additional account permissions needed

5. **Generate Token:**
   - Click "Generate token"
   - **IMPORTANT**: Copy the token immediately (you won't see it again)

### Option B: Classic Personal Access Token

1. **Go to GitHub Settings:**
   - Navigate to https://github.com/settings/tokens
   - Click "Generate new token" → "Generate new token (classic)"

2. **Configure Token Settings:**
   - **Note**: `SpringCRM GitOps Token`
   - **Expiration**: 90 days (or custom as needed)

3. **Select Scopes:**
   - ✅ **repo** (Full control of private repositories)
     - ✅ repo:status
     - ✅ repo_deployment
     - ✅ public_repo
   - ✅ **workflow** (Update GitHub Action workflows)

4. **Generate Token:**
   - Click "Generate token"
   - **IMPORTANT**: Copy the token immediately

## Step 2: Add Token to Repository Secrets

### Via GitHub Web Interface

1. **Navigate to Repository Settings:**
   - Go to your SpringCRM repository
   - Click "Settings" tab
   - Click "Secrets and variables" → "Actions"

2. **Add Repository Secret:**
   - Click "New repository secret"
   - **Name**: `GITOPS_TOKEN`
   - **Secret**: Paste the PAT you created
   - Click "Add secret"

3. **Verify Secret:**
   - The secret should appear in the list as `GITOPS_TOKEN`
   - The value will be hidden for security

### Via GitHub CLI (Alternative)

```bash
# Install GitHub CLI if not already installed
# Windows: winget install GitHub.cli
# macOS: brew install gh
# Linux: See https://cli.github.com/manual/installation

# Authenticate with GitHub CLI
gh auth login

# Add the secret to your repository
gh secret set GITOPS_TOKEN --body "your_token_here" --repo huylekha/SpringCRM
```

## Step 3: Verify Token Configuration

### Test with Manual Workflow Dispatch

1. **Go to Actions Tab:**
   - Navigate to your repository's Actions tab
   - Find the "Kubernetes GitOps CI/CD" workflow

2. **Run Manual Dispatch:**
   - Click "Run workflow"
   - Select environment: `dev`
   - Check "Force rebuild all services": `true`
   - Click "Run workflow"

3. **Monitor Workflow:**
   - Watch the workflow execution
   - Check the "gitops-update" job specifically
   - Look for successful git push operation

### Check Git Push Logs

In the workflow logs, look for:

```
🔄 Updating GitOps manifests...
✅ Updated image tags for environment: dev
📤 Pushing GitOps changes...
✅ GitOps changes pushed successfully
```

## Step 4: Troubleshooting Common Issues

### Issue 1: Authentication Failed

**Error:**
```
remote: Permission to huylekha/SpringCRM.git denied to github-actions[bot].
fatal: unable to access 'https://github.com/huylekha/SpringCRM.git/': The requested URL returned error: 403
```

**Solution:**
- Verify `GITOPS_TOKEN` secret exists and has correct value
- Ensure PAT has `repo` scope (classic) or `Contents: Write` permission (fine-grained)
- Check if repository is private and PAT has access

### Issue 2: Token Expired

**Error:**
```
remote: Invalid username or password.
fatal: Authentication failed for 'https://github.com/huylekha/SpringCRM.git/'
```

**Solution:**
- Generate new PAT with same permissions
- Update `GITOPS_TOKEN` secret with new value

### Issue 3: Protected Branch Rules

**Error:**
```
remote: error: GH006: Protected branch update failed
```

**Solution:**
- Add the PAT user to branch protection exceptions
- Or use `--force-with-lease` for rollback operations (already implemented)

### Issue 4: Rate Limiting

**Error:**
```
API rate limit exceeded for user
```

**Solution:**
- Use fine-grained PAT instead of classic PAT
- Reduce workflow frequency if running too often

## Step 5: Security Best Practices

### Token Security

1. **Minimal Permissions:**
   - Only grant necessary repository permissions
   - Avoid account-level permissions unless required

2. **Regular Rotation:**
   - Set expiration dates (90 days recommended)
   - Rotate tokens before expiration
   - Update repository secret with new token

3. **Monitor Usage:**
   - Check GitHub audit logs for token usage
   - Review workflow logs for authentication issues

### Repository Security

1. **Branch Protection:**
   - Enable branch protection for `main` and `develop`
   - Require PR reviews for sensitive changes
   - Enable status checks

2. **Secret Management:**
   - Never log secret values in workflows
   - Use `secrets.GITOPS_TOKEN` syntax consistently
   - Avoid hardcoding tokens in workflow files

## Step 6: Alternative Authentication Methods

### GitHub App (Enterprise Option)

For organizations, consider using a GitHub App instead of PAT:

```yaml
- name: Generate token
  id: generate_token
  uses: tibdex/github-app-token@v1
  with:
    app_id: ${{ secrets.APP_ID }}
    private_key: ${{ secrets.APP_PRIVATE_KEY }}

- name: Checkout code
  uses: actions/checkout@v4
  with:
    token: ${{ steps.generate_token.outputs.token }}
```

### Deploy Keys (Read-Only Alternative)

For read-only operations, use deploy keys:

```yaml
- name: Checkout code
  uses: actions/checkout@v4
  with:
    ssh-key: ${{ secrets.DEPLOY_KEY }}
```

## Step 7: Workflow Testing Checklist

After setting up `GITOPS_TOKEN`, verify these operations work:

### ✅ Basic Operations
- [ ] Workflow can checkout code with token
- [ ] Git push operations succeed
- [ ] Kustomize image tag updates are committed
- [ ] ArgoCD detects and syncs changes

### ✅ Branch Operations
- [ ] Push to `develop` branch (dev environment)
- [ ] Push to `release/*` branch (staging environment)
- [ ] Push to `main` branch (prod environment)

### ✅ Rollback Operations
- [ ] Manual rollback workflow executes
- [ ] Rollback commits are pushed successfully
- [ ] Force push with lease works for rollbacks

### ✅ Error Handling
- [ ] Workflow fails gracefully on auth errors
- [ ] Fallback to `github.token` works when `GITOPS_TOKEN` unavailable
- [ ] Error messages are clear and actionable

## Step 8: Monitoring and Maintenance

### Regular Maintenance Tasks

1. **Monthly Token Review:**
   ```bash
   # Check token expiration
   gh auth status
   
   # List repository secrets
   gh secret list --repo huylekha/SpringCRM
   ```

2. **Workflow Health Check:**
   ```bash
   # Check recent workflow runs
   gh run list --repo huylekha/SpringCRM --workflow="Kubernetes GitOps CI/CD"
   
   # View specific run details
   gh run view RUN_ID --repo huylekha/SpringCRM
   ```

3. **Git History Audit:**
   ```bash
   # Check GitOps commits
   git log --oneline --grep="gitops" --since="1 month ago"
   
   # Check rollback commits
   git log --oneline --grep="rollback" --since="1 month ago"
   ```

### Monitoring Alerts

Set up monitoring for:
- Failed workflow runs due to authentication
- Token expiration warnings (7 days before)
- Unusual git push activity
- Failed ArgoCD sync operations

## Conclusion

The `GITOPS_TOKEN` is essential for:
- ✅ Automated GitOps workflows
- ✅ Seamless CI/CD pipeline execution
- ✅ Reliable rollback operations
- ✅ Proper ArgoCD integration

With proper setup and maintenance, this token enables fully automated GitOps operations while maintaining security best practices.