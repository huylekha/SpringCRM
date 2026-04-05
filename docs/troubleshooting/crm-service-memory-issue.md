# CRM Service Memory Issue - Quick Fix Guide

## 🚨 Current Issue

The CRM Service is currently scaled down to 0 replicas due to insufficient memory in the Kubernetes cluster.

**Symptoms:**
- `curl http://dev.springcrm.com/crm-service/actuator/health` returns `503 Service Temporarily Unavailable`
- CRM pods stuck in `Pending` state with `Insufficient memory` error
- Multiple pods consuming cluster resources

## ✅ What's Already Fixed

1. **Database Tables Created**: All required tables (`idempotency_records`, `inbox_messages`, `outbox_messages`) have been manually created in the PostgreSQL database
2. **Resource Requirements Reduced**: CRM service now requires only 128Mi memory (down from 512Mi)
3. **Flyway Migration Issue Resolved**: Database schema is ready for CRM service

## 🔧 Quick Solutions

### Option 1: Restart Docker Desktop (Recommended)
```bash
# This will free up memory and restart the cluster
1. Right-click Docker Desktop in system tray
2. Select "Restart Docker Desktop"
3. Wait 2-3 minutes for cluster to be ready
4. Scale up CRM service:
   kubectl scale deployment dev-crm-service --replicas=1 -n dev
```

### Option 2: Free Up Memory by Scaling Down Other Services
```bash
# Scale down non-essential services temporarily
kubectl scale deployment dev-redis --replicas=0 -n dev
kubectl scale deployment dev-api-gateway --replicas=1 -n dev

# Then scale up CRM service
kubectl scale deployment dev-crm-service --replicas=1 -n dev

# Monitor the pod
kubectl get pods -n dev -w
```

### Option 3: Increase Docker Desktop Memory
```bash
1. Open Docker Desktop Settings
2. Go to Resources → Advanced
3. Increase Memory to 6GB or 8GB
4. Click "Apply & Restart"
5. Scale up CRM service:
   kubectl scale deployment dev-crm-service --replicas=1 -n dev
```

## 🧪 Verify CRM Service is Working

After applying any solution above:

```bash
# Check pod status
kubectl get pods -n dev | grep crm

# Check logs
kubectl logs -f deployment/dev-crm-service -n dev

# Test health endpoint
curl http://dev.springcrm.com/crm-service/actuator/health

# Expected response:
# {"groups":["liveness","readiness"],"status":"UP"}
```

## 📊 Monitor Resource Usage

```bash
# Check node resources (if metrics-server is available)
kubectl top nodes

# Check pod resources
kubectl top pods -n dev

# Check cluster events
kubectl get events -n dev --sort-by='.lastTimestamp'
```

## 🔄 Alternative: Use Legacy Path-Based URLs

While CRM service is down, you can still access other services via legacy URLs:

```bash
# These should work regardless of subdomain issues
curl http://springcrm.com/dev/auth-service/actuator/health
curl http://springcrm.com/dev/api-gateway/actuator/health
```

## 🎯 Expected Timeline

- **Immediate**: Other services (Frontend, API Gateway, Monitoring) are fully functional
- **5 minutes**: After Docker Desktop restart or memory increase
- **Manual intervention**: If persistent, consider reducing replicas of other services

## 📞 Support

If issues persist:
1. Check Docker Desktop has at least 4GB RAM allocated
2. Ensure no other heavy applications are running
3. Consider using `kind` or `k3d` instead of Docker Desktop for better resource management

---

**Status**: CRM Service database is ready, only waiting for sufficient memory resources to start the pod.