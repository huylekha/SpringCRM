# SpringCRM Local Domain Setup Guide

This guide explains how to configure the `springcrm.com` domain for local development and testing of the SpringCRM application.

## Overview

Instead of using `localhost` for accessing your services, you can configure a custom domain `springcrm.com` that points to your local machine. This provides:

- **Easier Testing**: Use memorable URLs like `http://springcrm.com/dev/frontend/`
- **Realistic Environment**: Mimics production domain setup
- **Better Cookie Handling**: Proper domain-based cookies and sessions
- **HTTPS Testing**: Self-signed certificates work better with custom domains

## Quick Setup

### Option 1: Automated Setup (Recommended)

Run the automated setup script as Administrator:

```powershell
# Open PowerShell as Administrator
# Navigate to your SpringCRM project directory
cd d:\Azure\Repo\SpringCRM

# Run the domain setup script
.\scripts\setup-local-domain.ps1
```

The script will:
- ✅ Add `springcrm.com` to your Windows hosts file
- ✅ Point the domain to `127.0.0.1` (localhost)
- ✅ Create a backup of your hosts file
- ✅ Flush DNS cache
- ✅ Show you test URLs

### Option 2: Manual Setup

If you prefer to set up manually:

1. **Open Notepad as Administrator**
2. **Open the hosts file**: `C:\Windows\System32\drivers\etc\hosts`
3. **Add this line at the end**:
   ```
   127.0.0.1    springcrm.com
   ```
4. **Save the file**
5. **Flush DNS cache**:
   ```powershell
   ipconfig /flushdns
   ```

## Test URLs

After setup, you can access your services using these URLs:

### Development Environment
- **Frontend**: http://springcrm.com/dev/frontend/
- **API Gateway**: http://springcrm.com/dev/api-gateway/actuator/health
- **Auth Service**: http://springcrm.com/dev/auth-service/actuator/health
- **CRM Service**: http://springcrm.com/dev/crm-service/actuator/health

### Staging Environment
- **Frontend**: http://springcrm.com/staging/frontend/
- **API Gateway**: http://springcrm.com/staging/api-gateway/actuator/health
- **Auth Service**: http://springcrm.com/staging/auth-service/actuator/health
- **CRM Service**: http://springcrm.com/staging/crm-service/actuator/health

### Production Environment (HTTPS)
- **Frontend**: https://springcrm.com/prod/frontend/
- **API Gateway**: https://springcrm.com/prod/api-gateway/actuator/health
- **Auth Service**: https://springcrm.com/prod/auth-service/actuator/health
- **CRM Service**: https://springcrm.com/prod/crm-service/actuator/health

## Verification

### Test Domain Resolution

```powershell
# Test if domain resolves correctly
nslookup springcrm.com

# Should return:
# Name:    springcrm.com
# Address: 127.0.0.1
```

### Test HTTP Connectivity

```powershell
# Test basic connectivity
curl http://springcrm.com/dev/auth-service/actuator/health

# Should return JSON health status
```

### Test HTTPS (Production)

```powershell
# Test HTTPS with self-signed certificate (ignore certificate warnings)
curl -k https://springcrm.com/prod/auth-service/actuator/health
```

## Troubleshooting

### Domain Not Resolving

**Problem**: `springcrm.com` doesn't resolve to localhost

**Solutions**:
1. **Check hosts file**: Ensure the entry exists and is correctly formatted
2. **Flush DNS cache**: Run `ipconfig /flushdns`
3. **Restart browser**: Close and reopen your browser
4. **Check antivirus**: Some antivirus software blocks hosts file changes

### Permission Denied

**Problem**: Cannot modify hosts file

**Solutions**:
1. **Run as Administrator**: Ensure PowerShell/Notepad is running as Administrator
2. **Disable antivirus temporarily**: Some antivirus software protects the hosts file
3. **Check file permissions**: Ensure you have write access to the hosts file

### Services Not Accessible

**Problem**: Domain resolves but services return errors

**Solutions**:
1. **Check Kubernetes**: Ensure your cluster is running and services are deployed
2. **Check Ingress**: Verify NGINX Ingress Controller is running
3. **Check ArgoCD**: Ensure applications are synced and healthy
4. **Check port forwarding**: If using port forwarding, ensure it's active

### HTTPS Certificate Warnings

**Problem**: Browser shows certificate warnings for HTTPS

**Expected Behavior**: This is normal for self-signed certificates

**Solutions**:
1. **Click "Advanced" → "Proceed to springcrm.com (unsafe)"** in Chrome
2. **Use curl with -k flag**: `curl -k https://springcrm.com/...`
3. **Add certificate to trusted store** (optional, for development only)

## Advanced Configuration

### Multiple Domains

You can add multiple domains for different purposes:

```
127.0.0.1    springcrm.com
127.0.0.1    springcrm.local
127.0.0.1    springcrm.dev
```

### Port-Specific Domains

For specific services on different ports:

```
127.0.0.1    api.springcrm.com
127.0.0.1    app.springcrm.com
127.0.0.1    admin.springcrm.com
```

### Custom TLD

Use a custom top-level domain:

```
127.0.0.1    springcrm.test
127.0.0.1    springcrm.local
127.0.0.1    springcrm.internal
```

## Cleanup

### Remove Domain Configuration

To remove the domain configuration:

```powershell
# Using the automated script
.\scripts\setup-local-domain.ps1 -Remove

# Or manually edit the hosts file and remove the line:
# 127.0.0.1    springcrm.com
```

### Restore Original Configuration

If you need to restore the original localhost configuration:

1. **Remove springcrm.com entries** from hosts file
2. **Update Kubernetes manifests** to use `localhost` instead of `springcrm.com`
3. **Redeploy applications** with updated configuration

## Integration with CI/CD

The GitHub Actions workflow has been updated to use `springcrm.com` for smoke tests:

```yaml
# Smoke tests now use springcrm.com
$BaseUrl = "http://springcrm.com"
$url = "$BaseUrl/$Environment/$service/actuator/health"
```

This ensures that CI/CD tests use the same domain configuration as manual testing.

## Security Considerations

### Local Development Only

- ✅ **Safe for local development**: Only affects your local machine
- ✅ **No external traffic**: Domain only resolves locally
- ✅ **Reversible**: Easy to remove when no longer needed

### Production Considerations

- ⚠️ **Don't use in production**: This is for local development only
- ⚠️ **Real domain required**: Production should use a real registered domain
- ⚠️ **Proper certificates**: Production should use CA-signed certificates

## Best Practices

1. **Document changes**: Keep track of hosts file modifications
2. **Use version control**: Consider backing up your hosts file
3. **Team consistency**: Share domain configuration with team members
4. **Environment separation**: Use different subdomains for different environments
5. **Regular cleanup**: Remove unused domain entries periodically

## Related Documentation

- [TLS Certificate Setup](tls-certificate-setup.md) - Configure HTTPS certificates
- [Post-Deployment Validation](post-deployment-validation-guide.md) - Test your setup
- [Manual Secrets Setup](manual-secrets-setup.md) - Configure secrets
- [Monitoring Stack Setup](monitoring-stack-setup.md) - Set up monitoring

## Support

If you encounter issues:

1. **Check the troubleshooting section** above
2. **Review the logs** in Kubernetes and ArgoCD
3. **Verify network connectivity** using curl or ping
4. **Check DNS resolution** using nslookup
5. **Consult team documentation** for environment-specific issues