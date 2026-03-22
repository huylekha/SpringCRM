# Advanced Kubernetes Features

This directory contains optional advanced features for the SpringCRM Kubernetes deployment. These features provide enhanced deployment strategies, scaling, monitoring, and observability capabilities.

## 📁 Directory Structure

```
k8s/advanced/
├── argo-rollouts/           # Advanced deployment strategies
│   ├── auth-service-rollout.yaml
│   ├── crm-service-rollout.yaml
│   └── api-gateway-rollout.yaml
├── hpa/                     # Horizontal Pod Autoscaling
│   ├── auth-service-hpa.yaml
│   ├── crm-service-hpa.yaml
│   └── api-gateway-hpa.yaml
├── monitoring/              # Enhanced monitoring
│   ├── prometheus/
│   │   └── servicemonitor.yaml
│   └── grafana/
│       └── springcrm-dashboard.json
├── observability/           # Distributed tracing
│   └── opentelemetry/
│       └── otel-collector.yaml
└── README.md               # This file
```

## 🚀 Quick Start

### Enable All Features
```bash
# Navigate to project root
cd /path/to/SpringCRM

# Apply all advanced features
kubectl apply -R -f k8s/advanced/
```

### Enable Specific Features
```bash
# Enable only HPA
kubectl apply -f k8s/advanced/hpa/

# Enable only monitoring
kubectl apply -f k8s/advanced/monitoring/

# Enable only Argo Rollouts
kubectl apply -f k8s/advanced/argo-rollouts/
```

## 🔄 Argo Rollouts

**Purpose:** Advanced deployment strategies with canary deployments, blue-green deployments, and automated rollbacks.

**Features:**
- Canary deployments with traffic splitting (20% → 40% → 60% → 80% → 100%)
- Manual approval gates
- Automated analysis and rollback
- Integration with Prometheus metrics

**Prerequisites:**
- Argo Rollouts controller installed
- NGINX Ingress Controller
- Prometheus (for analysis)

**Usage:**
```bash
# Install Argo Rollouts controller
kubectl create namespace argo-rollouts
kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml

# Apply rollout manifests
kubectl apply -f k8s/advanced/argo-rollouts/

# Monitor rollout progress
kubectl argo rollouts get rollout auth-service -n dev --watch

# Promote canary
kubectl argo rollouts promote auth-service -n dev
```

**Configuration:**
- **Traffic Split:** 20% → 40% → 60% → 80% → 100%
- **Analysis:** Success rate >95%, response time <500ms
- **Rollback:** Automatic on analysis failure

## 📈 Horizontal Pod Autoscaler (HPA)

**Purpose:** Automatic scaling based on CPU, memory, and custom metrics.

**Features:**
- CPU-based scaling (target: 70%)
- Memory-based scaling (target: 80%)
- Custom metrics scaling (RPS, queue length)
- Pod Disruption Budgets for availability

**Prerequisites:**
- Metrics Server installed
- Resource requests defined in pods

**Usage:**
```bash
# Install Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Apply HPA manifests
kubectl apply -f k8s/advanced/hpa/

# Check HPA status
kubectl get hpa -n dev
kubectl describe hpa auth-service-hpa -n dev
```

**Configuration:**
- **Min Replicas:** 2 (dev), 3 (prod)
- **Max Replicas:** 10
- **Scale Up:** Max 100% increase, 2 pods per minute
- **Scale Down:** Max 50% decrease, 1 pod per minute, 5-minute stabilization

## 📊 Enhanced Monitoring

**Purpose:** Comprehensive monitoring with Prometheus alerts and Grafana dashboards.

### Prometheus ServiceMonitor

**Features:**
- Automatic service discovery
- Metrics scraping from `/actuator/prometheus`
- Alert rules for common issues

**Alerts:**
- High error rate (>5% for 5 minutes)
- High latency (95th percentile >1s)
- Service down (>1 minute)
- High memory usage (>85%)
- Database pool exhaustion (>90%)
- High GC pause frequency

**Usage:**
```bash
# Apply ServiceMonitor and alerts
kubectl apply -f k8s/advanced/monitoring/prometheus/

# Check if alerts are loaded
kubectl get prometheusrule -n monitoring
```

### Grafana Dashboard

**Features:**
- Request rate and error rate visualization
- Response time percentiles
- JVM memory and GC metrics
- Database connection pool status
- Real-time statistics

**Usage:**
```bash
# Import dashboard
kubectl create configmap springcrm-dashboard \
  --from-file=k8s/advanced/monitoring/grafana/springcrm-dashboard.json \
  -n monitoring

# Label for auto-discovery
kubectl label configmap springcrm-dashboard grafana_dashboard=1 -n monitoring
```

## 🔍 OpenTelemetry Observability

**Purpose:** Distributed tracing and comprehensive observability.

**Features:**
- OTLP trace collection
- Prometheus metrics collection
- Jaeger integration
- Service dependency mapping

**Prerequisites:**
- OpenTelemetry Collector
- Jaeger or other tracing backend
- Application instrumentation

**Usage:**
```bash
# Create observability namespace
kubectl create namespace observability

# Install Jaeger
kubectl create -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.50.0/jaeger-operator.yaml -n observability

# Deploy OpenTelemetry Collector
kubectl apply -f k8s/advanced/observability/opentelemetry/

# Access Jaeger UI
kubectl port-forward svc/jaeger-query -n observability 16686:16686
```

**Configuration:**
- **Trace Sampling:** 10% (configurable)
- **Batch Processing:** 1024 spans, 1s timeout
- **Exporters:** Jaeger, Prometheus, OTLP
- **Resource Limits:** 512Mi memory, 200m CPU

## 🔧 Environment-Specific Configuration

### Development Environment
```bash
# Enable basic features for development
kubectl apply -f k8s/advanced/hpa/ -n dev
kubectl apply -f k8s/advanced/monitoring/prometheus/ -n dev

# Reduced resource limits
# Min replicas: 1, Max replicas: 5
```

### Staging Environment
```bash
# Enable most features for staging
kubectl apply -f k8s/advanced/hpa/ -n staging
kubectl apply -f k8s/advanced/monitoring/ -n staging
kubectl apply -f k8s/advanced/argo-rollouts/ -n staging

# Production-like configuration
# Min replicas: 2, Max replicas: 8
```

### Production Environment
```bash
# Enable all features for production
kubectl apply -R -f k8s/advanced/ -n prod

# Full observability and safety
# Min replicas: 3, Max replicas: 10
# Canary deployments with analysis
# Full monitoring and alerting
```

## 📋 Feature Toggle Checklist

### ✅ Enable Features

**HPA (Horizontal Pod Autoscaler):**
- [ ] Install Metrics Server
- [ ] Apply HPA manifests
- [ ] Verify scaling behavior
- [ ] Configure Pod Disruption Budgets

**Prometheus Monitoring:**
- [ ] Apply ServiceMonitor
- [ ] Configure alert rules
- [ ] Test alert notifications
- [ ] Create runbook documentation

**Grafana Dashboards:**
- [ ] Import dashboard ConfigMap
- [ ] Verify dashboard visibility
- [ ] Customize panels if needed
- [ ] Set up dashboard alerts

**Argo Rollouts:**
- [ ] Install Argo Rollouts controller
- [ ] Replace Deployments with Rollouts
- [ ] Configure canary analysis
- [ ] Test rollout and rollback procedures

**OpenTelemetry:**
- [ ] Deploy OpenTelemetry Collector
- [ ] Install tracing backend (Jaeger)
- [ ] Configure application instrumentation
- [ ] Verify end-to-end tracing

### ❌ Disable Features

**HPA:**
```bash
kubectl delete -f k8s/advanced/hpa/
kubectl scale deployment auth-service --replicas=2 -n dev
```

**Monitoring:**
```bash
kubectl delete -f k8s/advanced/monitoring/prometheus/
kubectl delete configmap springcrm-dashboard -n monitoring
```

**Argo Rollouts:**
```bash
kubectl delete -f k8s/advanced/argo-rollouts/
kubectl apply -f k8s/base/  # Restore standard deployments
```

**OpenTelemetry:**
```bash
kubectl delete -f k8s/advanced/observability/opentelemetry/
kubectl delete namespace observability
```

## 🚨 Rollback Procedures

### Emergency Rollback
```bash
# Disable all advanced features
kubectl delete -R -f k8s/advanced/

# Restore basic deployments
kubectl apply -f k8s/base/

# Verify services are running
kubectl get pods -n dev
kubectl get pods -n staging  
kubectl get pods -n prod
```

### Selective Rollback
```bash
# Rollback specific feature
kubectl delete -f k8s/advanced/argo-rollouts/
kubectl argo rollouts abort auth-service -n dev

# Restore standard deployment
kubectl apply -f k8s/base/auth-service/deployment.yaml
```

## 🔍 Troubleshooting

### Common Issues

**HPA not scaling:**
```bash
# Check metrics server
kubectl get apiservice v1beta1.metrics.k8s.io

# Check resource usage
kubectl top pods -n dev

# Check HPA status
kubectl describe hpa auth-service-hpa -n dev
```

**Prometheus not scraping:**
```bash
# Check ServiceMonitor
kubectl get servicemonitor -n monitoring

# Check service labels
kubectl get service auth-service -n dev --show-labels

# Check Prometheus targets
kubectl port-forward svc/prometheus-server -n monitoring 9090:80
```

**Rollout stuck:**
```bash
# Check rollout status
kubectl argo rollouts get rollout auth-service -n dev

# Check analysis results
kubectl get analysisrun -n dev

# Promote manually if needed
kubectl argo rollouts promote auth-service -n dev
```

**Tracing not working:**
```bash
# Check OpenTelemetry Collector
kubectl logs deployment/otel-collector -n observability

# Check Jaeger
kubectl port-forward svc/jaeger-query -n observability 16686:16686

# Verify application configuration
kubectl exec deployment/auth-service -n dev -- env | grep OTEL
```

## 📊 Resource Requirements

### Minimum Requirements
- **CPU:** Additional 500m across all features
- **Memory:** Additional 2Gi across all features
- **Storage:** 10Gi for monitoring data

### Recommended Requirements
- **CPU:** Additional 1000m for full observability
- **Memory:** Additional 4Gi for full observability
- **Storage:** 50Gi for long-term monitoring data

### Per-Feature Overhead
| Feature | CPU | Memory | Storage |
|---------|-----|--------|---------|
| HPA | ~1m | ~10Mi | - |
| Prometheus | 100-500m | 1-4Gi | 10-50Gi |
| Grafana | 50-200m | 200-500Mi | 1-5Gi |
| Argo Rollouts | 50m | 128Mi | - |
| OpenTelemetry | 100-200m | 256-512Mi | - |

## 🔗 Related Documentation

- [Advanced Features Guide](../../docs/devops/advanced-features-guide.md) - Comprehensive setup guide
- [Kubernetes GitOps Setup](../../docs/devops/k8s-gitops-setup.md) - Base system setup
- [Spring Boot K8s Configuration](../../docs/devops/spring-boot-k8s-config.md) - Application configuration
- [ArgoCD Applications](../../infra/argocd/README.md) - GitOps configuration

## 🎯 Best Practices

1. **Start Small:** Enable HPA and basic monitoring first
2. **Test Thoroughly:** Validate each feature in development before production
3. **Monitor Resources:** Track resource usage after enabling features
4. **Document Changes:** Update runbooks and documentation
5. **Plan Rollbacks:** Always have a rollback plan before enabling features
6. **Gradual Rollout:** Enable features in dev → staging → prod order