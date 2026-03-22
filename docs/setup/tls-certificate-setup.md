# TLS Certificate Setup Guide

This guide explains how to create self-signed TLS certificates for local Kubernetes deployment.

## Prerequisites

- OpenSSL installed on your system
- kubectl configured to access your local Kubernetes cluster
- The `prod` namespace must exist in your cluster

## Step 1: Create the Production Namespace (if not exists)

```bash
kubectl create namespace prod --dry-run=client -o yaml | kubectl apply -f -
```

## Step 2: Generate Self-Signed Certificate

### Option A: Using OpenSSL (Recommended)

```bash
# Create a private key
openssl genrsa -out springcrm-tls.key 2048

# Create a certificate signing request (CSR)
openssl req -new -key springcrm-tls.key -out springcrm-tls.csr -subj "/CN=springcrm.com/O=SpringCRM"

# Generate the self-signed certificate (valid for 365 days)
openssl x509 -req -in springcrm-tls.csr -signkey springcrm-tls.key -out springcrm-tls.crt -days 365

# Clean up the CSR file
rm springcrm-tls.csr
```

### Option B: Using OpenSSL with Subject Alternative Names (SAN)

If you need to support multiple hostnames:

```bash
# Create a config file for SAN
cat > springcrm-tls.conf <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
CN = springcrm.com
O = SpringCRM

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = springcrm.com
DNS.2 = localhost
IP.1 = 127.0.0.1
EOF

# Generate private key
openssl genrsa -out springcrm-tls.key 2048

# Generate certificate with SAN
openssl req -new -x509 -key springcrm-tls.key -out springcrm-tls.crt -days 365 -config springcrm-tls.conf -extensions v3_req

# Clean up config file
rm springcrm-tls.conf
```

## Step 3: Create Kubernetes TLS Secret

```bash
# Create the TLS secret in the prod namespace
kubectl create secret tls springcrm-tls-secret \
  --cert=springcrm-tls.crt \
  --key=springcrm-tls.key \
  --namespace=prod

# Verify the secret was created
kubectl get secret springcrm-tls-secret -n prod
```

## Step 4: Clean Up Certificate Files

**Important**: Remove the certificate files from your local filesystem for security:

```bash
rm springcrm-tls.key springcrm-tls.crt
```

The certificate is now safely stored in the Kubernetes secret.

## Step 5: Verify TLS Configuration

After deploying your application:

```bash
# Check if the ingress is using the TLS secret
kubectl get ingress springcrm-ingress -n prod -o yaml | grep -A 5 tls

# Test HTTPS access (ignore certificate warnings for self-signed)
curl -k https://localhost/prod/frontend/
```

## Troubleshooting

### Certificate Not Trusted

Since this is a self-signed certificate, browsers will show security warnings. For local development:

1. **Chrome/Edge**: Click "Advanced" → "Proceed to localhost (unsafe)"
2. **Firefox**: Click "Advanced" → "Accept the Risk and Continue"
3. **curl**: Use the `-k` flag to ignore certificate errors

### Certificate Renewal

Self-signed certificates expire after the specified days (365 in our example). To renew:

1. Generate a new certificate using the same steps
2. Update the Kubernetes secret:
   ```bash
   kubectl create secret tls springcrm-tls-secret \
     --cert=springcrm-tls-new.crt \
     --key=springcrm-tls-new.key \
     --namespace=prod \
     --dry-run=client -o yaml | kubectl apply -f -
   ```

### ArgoCD and TLS Secrets

The ArgoCD application is configured to ignore differences in Secret data to prevent overwriting manually created certificates. This means:

- ✅ ArgoCD will not overwrite your TLS secret
- ✅ You can update certificates manually without Git commits
- ⚠️ Remember to backup your certificates before cluster maintenance

## Security Notes

1. **Never commit private keys to Git repositories**
2. **Use proper CA-signed certificates in real production environments**
3. **Rotate certificates regularly (every 90-365 days)**
4. **Consider using cert-manager for automatic certificate management in production**

## Production Considerations

For real production deployments, consider:

1. **Let's Encrypt with cert-manager**: Automatic certificate provisioning and renewal
2. **Commercial CA certificates**: For public-facing applications
3. **Wildcard certificates**: For multiple subdomains
4. **Certificate monitoring**: Alerts for certificate expiration

Example cert-manager setup for production:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: admin@yourdomain.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
```