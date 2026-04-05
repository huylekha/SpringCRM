# SpringCRM Monitoring Access Guide

## 🌐 **Subdomain-based Access (New Architecture)**

All monitoring services are now accessible via dedicated subdomains for better isolation and cleaner URLs:

### **Grafana Dashboard**
- **Primary URL**: http://grafana.springcrm.com
- **Legacy URL**: http://springcrm.com/grafana (still supported)
- **Production URL**: https://grafana.springcrm.com
- **Username**: `admin`
- **Password**: `bQSXGbjKBRriqEqvjlAy8RrKUooph79gJVb6PfZl`
- **Features**: 
  - Kubernetes cluster monitoring
  - Application metrics
  - Custom dashboards
  - Alerting

### **Prometheus**
- **Primary URL**: http://prometheus.springcrm.com
- **Legacy URL**: http://springcrm.com/prometheus (still supported)
- **Production URL**: https://prometheus.springcrm.com
- **Features**:
  - Metrics collection
  - Query interface
  - Target discovery
  - Rules management

### **AlertManager**
- **Primary URL**: http://alertmanager.springcrm.com
- **Production URL**: https://alertmanager.springcrm.com
- **Features**:
  - Alert handling
  - Notification routing
  - Silence management

### **Docker Registry**
- **URL**: http://localhost:5000
- **API Catalog**: http://localhost:5000/v2/_catalog
- **Repositories**: 
  - `auth-service`
  - `crm-service` 
  - `api-gateway`
  - `frontend`

## 🚀 **SpringCRM Application Services**

### **Development Environment**
- **Main App**: http://springcrm.com
- **API Gateway**: http://api.springcrm.com
- **Auth Service**: http://dev.springcrm.com/auth-service/actuator/health
- **CRM Service**: http://dev.springcrm.com/crm-service/actuator/health
- **Legacy URLs** (still supported):
  - Auth Service: http://springcrm.com/dev/auth-service/actuator/health
  - CRM Service: http://springcrm.com/dev/crm-service/actuator/health
  - API Gateway: http://springcrm.com/dev/api-gateway/actuator/health
  - Frontend: http://springcrm.com/dev/frontend/api/health

### **Staging Environment**
- **Main App**: http://staging.springcrm.com
- **API Gateway**: http://api.springcrm.com (shared with dev for local testing)
- **Auth Service**: http://staging.springcrm.com/auth-service/actuator/health
- **CRM Service**: http://staging.springcrm.com/crm-service/actuator/health
- **Legacy URLs** (still supported):
  - Auth Service: http://springcrm.com/staging/auth-service/actuator/health
  - CRM Service: http://springcrm.com/staging/crm-service/actuator/health
  - API Gateway: http://springcrm.com/staging/api-gateway/actuator/health
  - Frontend: http://springcrm.com/staging/frontend/api/health

### **Production Environment**
- **Main App**: https://springcrm.com
- **API Gateway**: https://api.springcrm.com
- **Auth Service**: https://prod.springcrm.com/auth-service/actuator/health
- **CRM Service**: https://prod.springcrm.com/crm-service/actuator/health
- **Legacy URLs** (still supported):
  - Auth Service: https://springcrm.com/prod/auth-service/actuator/health
  - CRM Service: https://springcrm.com/prod/crm-service/actuator/health
  - API Gateway: https://springcrm.com/prod/api-gateway/actuator/health
  - Frontend: https://springcrm.com/prod/frontend/api/health

## 🔧 **Technical Details**

### **Ingress Configuration**
- **Monitoring Subdomain Ingress**: `k8s/ingress/monitoring-subdomain-ingress.yaml`
- **Main App Ingress**: `k8s/overlays/{env}/main-app-ingress-patch.yaml`
- **API Gateway Ingress**: `k8s/overlays/{env}/api-ingress-patch.yaml`
- **Environment Ingress**: `k8s/overlays/{env}/subdomain-ingress.yaml`
- **Legacy Ingress**: `k8s/overlays/{env}/ingress-patch.yaml` (backward compatibility)

### **Services Configuration**
- **Grafana**: ClusterIP service with Ingress routing
- **Prometheus**: ClusterIP service with Ingress routing
- **Applications**: ClusterIP services with environment-specific routing

### **Persistence**
- **Grafana**: Persistent storage for dashboards and settings
- **Prometheus**: Persistent storage for metrics data
- **Applications**: Database and Redis persistence

## 🔄 **Auto-restart after System Reboot**

Các services sẽ tự động khởi động lại khi restart máy:
- ✅ Kubernetes cluster
- ✅ Grafana và Prometheus
- ✅ SpringCRM applications
- ✅ Docker registry
- ✅ Ingress routing

## 📊 **Default Dashboards Available**

Grafana đã được cài đặt với các dashboards mặc định:
- Kubernetes Cluster Overview
- Node Exporter Full
- Kubernetes Pods
- Kubernetes Deployments
- Prometheus Stats

## 🔍 **Troubleshooting**

### **Kiểm tra services status:**
```bash
kubectl get pods -n monitoring
kubectl get pods -n dev
kubectl get ingress --all-namespaces
```

### **Restart services nếu cần:**
```bash
kubectl rollout restart deployment -n monitoring
kubectl rollout restart deployment -n dev
```

### **Xem logs:**
```bash
kubectl logs -n monitoring deployment/kube-prometheus-stack-grafana
kubectl logs -n dev deployment/dev-crm-service
```

---

## 🎯 **Service Discovery Dashboard**

For a comprehensive view of all services and real-time health monitoring:

- **Dashboard URL**: [docs/service-dashboard.html](docs/service-dashboard.html)
- **Features**:
  - Real-time health checks for all services
  - Environment switching (dev/staging/prod)
  - Copy-to-clipboard URLs
  - Quick setup commands
  - Service documentation links

## 🔗 **Quick Access URLs**

### **New Subdomain Architecture**
- **Main App**: http://springcrm.com
- **API Gateway**: http://api.springcrm.com/v1
- **Grafana**: http://grafana.springcrm.com
- **Prometheus**: http://prometheus.springcrm.com
- **AlertManager**: http://alertmanager.springcrm.com

### **Environment-Specific URLs**
- **Development**: http://dev.springcrm.com
- **Staging**: http://staging.springcrm.com
- **Production**: https://prod.springcrm.com

### **Setup Commands**
```powershell
# Setup all subdomains (run as Administrator)
.\scripts\setup-local-domain.ps1 -BatchSetup

# Generate TLS certificate for production
.\scripts\generate-wildcard-cert.ps1 -ApplyToCluster

# Apply new ingress configurations
kubectl apply -f k8s/ingress/monitoring-subdomain-ingress.yaml
kubectl apply -k k8s/overlays/dev
```

## 📚 **Migration Guide**

For detailed migration instructions from path-based to subdomain-based routing:
- **Migration Guide**: [docs/setup/subdomain-migration-guide.md](docs/setup/subdomain-migration-guide.md)

---

**Lưu ý**: The new subdomain architecture provides better service isolation, cleaner URLs, and improved security. Legacy URLs remain supported during the migration period.