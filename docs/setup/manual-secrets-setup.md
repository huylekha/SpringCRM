# Manual Secrets Setup Guide

This guide explains how to manually create Kubernetes secrets for production and staging environments. These secrets will not be overwritten by ArgoCD due to the `ignoreDifferences` configuration.

## Prerequisites

- kubectl configured to access your local Kubernetes cluster
- The `prod` and `staging` namespaces must exist
- Generate strong passwords and secrets (see generation commands below)

## Step 1: Create Namespaces (if not exists)

```bash
kubectl create namespace prod --dry-run=client -o yaml | kubectl apply -f -
kubectl create namespace staging --dry-run=client -o yaml | kubectl apply -f -
```

## Step 2: Generate Strong Secrets

### Generate Random Passwords

```bash
# Generate database password (32 characters)
DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
echo "Generated DB Password: $DB_PASSWORD"

# Generate JWT secret (64+ characters)
JWT_SECRET=$(openssl rand -base64 64 | tr -d "=+/" | cut -c1-64)
echo "Generated JWT Secret: $JWT_SECRET"

# Generate NextAuth secret (32 characters)
NEXTAUTH_SECRET=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
echo "Generated NextAuth Secret: $NEXTAUTH_SECRET"
```

**Important**: Save these values securely and use them in the commands below.

## Step 3: Production Environment Secrets

### 3.1 Auth Service Secret

```bash
kubectl create secret generic auth-service-secret \
  --from-literal=DB_PASSWORD="YOUR_PRODUCTION_DB_PASSWORD" \
  --from-literal=JWT_SECRET="YOUR_PRODUCTION_JWT_SECRET_64_CHARS_OR_MORE" \
  --from-literal=SENTRY_DSN="YOUR_PRODUCTION_SENTRY_DSN_OR_EMPTY" \
  --namespace=prod
```

### 3.2 CRM Service Secret

```bash
kubectl create secret generic crm-service-secret \
  --from-literal=DB_PASSWORD="YOUR_PRODUCTION_DB_PASSWORD" \
  --from-literal=SENTRY_DSN="YOUR_PRODUCTION_SENTRY_DSN_OR_EMPTY" \
  --namespace=prod
```

### 3.3 PostgreSQL Secret

```bash
kubectl create secret generic postgres-secret \
  --from-literal=POSTGRES_PASSWORD="YOUR_PRODUCTION_DB_PASSWORD" \
  --namespace=prod
```

### 3.4 Frontend Secret

```bash
kubectl create secret generic frontend-secret \
  --from-literal=NEXTAUTH_SECRET="YOUR_PRODUCTION_NEXTAUTH_SECRET_32_CHARS" \
  --from-literal=SENTRY_DSN="YOUR_PRODUCTION_SENTRY_DSN_OR_EMPTY" \
  --namespace=prod
```

## Step 4: Staging Environment Secrets

### 4.1 Auth Service Secret

```bash
kubectl create secret generic auth-service-secret \
  --from-literal=DB_PASSWORD="YOUR_STAGING_DB_PASSWORD" \
  --from-literal=JWT_SECRET="YOUR_STAGING_JWT_SECRET_64_CHARS_OR_MORE" \
  --from-literal=SENTRY_DSN="YOUR_STAGING_SENTRY_DSN_OR_EMPTY" \
  --namespace=staging
```

### 4.2 CRM Service Secret

```bash
kubectl create secret generic crm-service-secret \
  --from-literal=DB_PASSWORD="YOUR_STAGING_DB_PASSWORD" \
  --from-literal=SENTRY_DSN="YOUR_STAGING_SENTRY_DSN_OR_EMPTY" \
  --namespace=staging
```

### 4.3 PostgreSQL Secret

```bash
kubectl create secret generic postgres-secret \
  --from-literal=POSTGRES_PASSWORD="YOUR_STAGING_DB_PASSWORD" \
  --namespace=staging
```

### 4.4 Frontend Secret

```bash
kubectl create secret generic frontend-secret \
  --from-literal=NEXTAUTH_SECRET="YOUR_STAGING_NEXTAUTH_SECRET_32_CHARS" \
  --from-literal=SENTRY_DSN="YOUR_STAGING_SENTRY_DSN_OR_EMPTY" \
  --namespace=staging
```

## Step 5: Verify Secrets Creation

### Check Production Secrets

```bash
kubectl get secrets -n prod
kubectl describe secret auth-service-secret -n prod
kubectl describe secret crm-service-secret -n prod
kubectl describe secret postgres-secret -n prod
kubectl describe secret frontend-secret -n prod
```

### Check Staging Secrets

```bash
kubectl get secrets -n staging
kubectl describe secret auth-service-secret -n staging
kubectl describe secret crm-service-secret -n staging
kubectl describe secret postgres-secret -n staging
kubectl describe secret frontend-secret -n staging
```

## Step 6: Test Secret Values (Optional)

**Warning**: Only run these commands in a secure environment, as they will display secret values.

```bash
# Test production auth-service secret
kubectl get secret auth-service-secret -n prod -o jsonpath='{.data.DB_PASSWORD}' | base64 -d
echo

# Test staging JWT secret
kubectl get secret auth-service-secret -n staging -o jsonpath='{.data.JWT_SECRET}' | base64 -d
echo
```

## Secret Management Best Practices

### 1. Password Requirements

- **DB_PASSWORD**: At least 16 characters, alphanumeric + symbols
- **JWT_SECRET**: At least 64 characters for security
- **NEXTAUTH_SECRET**: Exactly 32 characters
- **SENTRY_DSN**: Valid Sentry project DSN or empty string

### 2. Environment Separation

- Use different passwords for prod vs staging
- Never use staging secrets in production
- Rotate secrets regularly (every 90 days recommended)

### 3. Secret Rotation

To update a secret:

```bash
# Example: Update production DB password
kubectl create secret generic auth-service-secret \
  --from-literal=DB_PASSWORD="NEW_PASSWORD" \
  --from-literal=JWT_SECRET="EXISTING_JWT_SECRET" \
  --from-literal=SENTRY_DSN="EXISTING_SENTRY_DSN" \
  --namespace=prod \
  --dry-run=client -o yaml | kubectl apply -f -

# Restart affected deployments
kubectl rollout restart deployment/prod-auth-service -n prod
kubectl rollout restart deployment/prod-crm-service -n prod
kubectl rollout restart deployment/prod-postgres -n prod
```

### 4. Backup and Recovery

```bash
# Backup all secrets (store securely)
kubectl get secrets -n prod -o yaml > prod-secrets-backup.yaml
kubectl get secrets -n staging -o yaml > staging-secrets-backup.yaml

# Restore from backup
kubectl apply -f prod-secrets-backup.yaml
kubectl apply -f staging-secrets-backup.yaml
```

## ArgoCD Integration

### How ArgoCD Handles These Secrets

1. **ignoreDifferences**: ArgoCD ignores changes to Secret `data` and `stringData` fields
2. **No Overwrite**: Manually created secrets won't be overwritten by Git changes
3. **Sync Safety**: ArgoCD can sync other resources without affecting secrets

### Verify ArgoCD Configuration

```bash
# Check if ignoreDifferences is configured
kubectl get application springcrm-prod -n argocd -o yaml | grep -A 10 ignoreDifferences
kubectl get application springcrm-staging -n argocd -o yaml | grep -A 10 ignoreDifferences
```

## Troubleshooting

### Secret Not Found

```bash
# Check if secret exists
kubectl get secret SECRET_NAME -n NAMESPACE

# Check secret data keys
kubectl get secret SECRET_NAME -n NAMESPACE -o jsonpath='{.data}' | jq keys
```

### Pod Cannot Access Secret

```bash
# Check pod environment variables
kubectl exec -it POD_NAME -n NAMESPACE -- env | grep SECRET_KEY

# Check secret mounting
kubectl describe pod POD_NAME -n NAMESPACE | grep -A 5 -B 5 Secret
```

### ArgoCD Sync Issues

```bash
# Force ArgoCD to ignore secret differences
kubectl patch application springcrm-prod -n argocd --type='merge' -p='{"spec":{"ignoreDifferences":[{"group":"","kind":"Secret","jsonPointers":["/data","/stringData"]}]}}'
```

## Security Considerations

1. **Never commit secrets to Git**: The placeholder files in Git should never contain real secrets
2. **Use RBAC**: Limit who can view/edit secrets in production
3. **Audit access**: Monitor secret access in production environments
4. **Encryption at rest**: Ensure Kubernetes etcd encryption is enabled
5. **Network security**: Use network policies to limit secret access

## Production Alternatives

For production environments, consider:

1. **External Secrets Operator**: Sync secrets from external systems (AWS Secrets Manager, HashiCorp Vault)
2. **Sealed Secrets**: Encrypt secrets that can be safely stored in Git
3. **SOPS**: Encrypt YAML files with age/PGP keys
4. **Cloud Provider Secrets**: Use cloud-native secret management services

Example External Secrets Operator configuration:

```yaml
apiVersion: external-secrets.io/v1beta1
kind: SecretStore
metadata:
  name: vault-backend
  namespace: prod
spec:
  provider:
    vault:
      server: "https://vault.example.com"
      path: "secret"
      version: "v2"
      auth:
        kubernetes:
          mountPath: "kubernetes"
          role: "springcrm-prod"
```