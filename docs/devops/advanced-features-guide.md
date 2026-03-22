# Advanced Features Guide

This guide covers optional advanced features for the SpringCRM Kubernetes GitOps system, including Argo Rollouts, HPA, advanced monitoring, and observability.

## 🎯 Overview

The advanced features provide:
- **Argo Rollouts:** Advanced deployment strategies (canary, blue-green)
- **Horizontal Pod Autoscaler (HPA):** Automatic scaling based on metrics
- **Enhanced Monitoring:** Prometheus alerts and Grafana dashboards
- **OpenTelemetry:** Distributed tracing and observability
- **Service Mesh:** Advanced traffic management (optional)

## 🚀 Feature Matrix

| Feature | Complexity | Benefits | Prerequisites |
|---------|------------|----------|---------------|
| HPA | Low | Auto-scaling, cost optimization | Metrics Server |
| Prometheus Alerts | Low | Proactive monitoring | Prometheus |
| Grafana Dashboards | Low | Visual monitoring | Grafana |
| Argo Rollouts | Medium | Safe deployments, canary testing | ArgoCD |
| OpenTelemetry | Medium | Distributed tracing, deep insights | OTLP endpoint |
| Service Mesh | High | Advanced traffic control, security | Istio/Linkerd |

## 📋 Quick Enable/Disable Checklist

### ✅ Enable Features

```bash
# Enable HPA (Horizontal Pod Autoscaler)
kubectl apply -f k8s/advanced/hpa/

# Enable Prometheus monitoring
kubectl apply -f k8s/advanced/monitoring/prometheus/

# Enable Grafana dashboards
kubectl apply -f k8s/advanced/monitoring/grafana/

# Enable Argo Rollouts
kubectl apply -f k8s/advanced/argo-rollouts/

# Enable OpenTelemetry
kubectl apply -f k8s/advanced/observability/opentelemetry/
```

### ❌ Disable Features

```bash
# Disable HPA
kubectl delete -f k8s/advanced/hpa/

# Disable Prometheus monitoring
kubectl delete -f k8s/advanced/monitoring/prometheus/

# Disable Grafana dashboards
kubectl delete configmap springcrm-dashboard -n monitoring

# Disable Argo Rollouts
kubectl delete -f k8s/advanced/argo-rollouts/

# Disable OpenTelemetry
kubectl delete -f k8s/advanced/observability/opentelemetry/
```

## 🔄 Argo Rollouts (Advanced Deployments)

### What it provides:
- Canary deployments with traffic splitting
- Blue-green deployments
- Automated rollback based on metrics
- Manual approval gates
- A/B testing capabilities

### Prerequisites:
- ArgoCD installed
- NGINX Ingress Controller
- Prometheus (for analysis)

### Setup Steps:

1. **Install Argo Rollouts Controller:**
   ```bash
   kubectl create namespace argo-rollouts
   kubectl apply -n argo-rollouts -f https://github.com/argoproj/argo-rollouts/releases/latest/download/install.yaml
   ```

2. **Install Argo Rollouts CLI:**
   ```bash
   # Windows
   choco install argo-rollouts
   
   # macOS
   brew install argoproj/tap/kubectl-argo-rollouts
   
   # Linux
   curl -LO https://github.com/argoproj/argo-rollouts/releases/latest/download/kubectl-argo-rollouts-linux-amd64
   chmod +x ./kubectl-argo-rollouts-linux-amd64
   sudo mv ./kubectl-argo-rollouts-linux-amd64 /usr/local/bin/kubectl-argo-rollouts
   ```

3. **Replace Deployments with Rollouts:**
   ```bash
   # Apply rollout manifests
   kubectl apply -f k8s/advanced/argo-rollouts/
   
   # Monitor rollout
   kubectl argo rollouts get rollout auth-service -n dev --watch
   ```

4. **Trigger Canary Deployment:**
   ```bash
   # Update image (this would normally be done by CI/CD)
   kubectl argo rollouts set image auth-service auth-service=localhost:5000/auth-service:new-version -n dev
   
   # Promote canary manually
   kubectl argo rollouts promote auth-service -n dev
   
   # Abort rollout if needed
   kubectl argo rollouts abort auth-service -n dev
   ```

### Configuration Options:

```yaml
# Canary strategy with 20%, 40%, 60%, 80% traffic split
strategy:
  canary:
    steps:
    - setWeight: 20
    - pause: {}  # Manual approval
    - setWeight: 40
    - pause: {duration: 30s}
    - setWeight: 60
    - pause: {duration: 30s}
    - setWeight: 80
    - pause: {duration: 30s}
```

## 📈 Horizontal Pod Autoscaler (HPA)

### What it provides:
- Automatic scaling based on CPU/memory usage
- Custom metrics scaling (RPS, queue length)
- Cost optimization through right-sizing
- Improved performance during traffic spikes

### Prerequisites:
- Metrics Server installed
- Resource requests defined in pods
- Custom metrics API (optional)

### Setup Steps:

1. **Install Metrics Server:**
   ```bash
   kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
   ```

2. **Apply HPA manifests:**
   ```bash
   kubectl apply -f k8s/advanced/hpa/
   ```

3. **Verify HPA status:**
   ```bash
   kubectl get hpa -n dev
   kubectl describe hpa auth-service-hpa -n dev
   ```

4. **Test scaling:**
   ```bash
   # Generate load to trigger scaling
   kubectl run -i --tty load-generator --rm --image=busybox --restart=Never -- /bin/sh
   # Inside the pod:
   while true; do wget -q -O- http://auth-service.dev.svc.cluster.local:8081/actuator/health; done
   ```

### Configuration Options:

```yaml
# Scale based on CPU, memory, and custom metrics
metrics:
- type: Resource
  resource:
    name: cpu
    target:
      type: Utilization
      averageUtilization: 70
- type: Pods
  pods:
    metric:
      name: http_requests_per_second
    target:
      type: AverageValue
      averageValue: "100"
```

## 📊 Enhanced Monitoring

### Prometheus Alerts

**What it provides:**
- Proactive alerting on service issues
- SLA monitoring and reporting
- Automated incident detection
- Integration with PagerDuty, Slack, etc.

**Setup:**
```bash
# Apply ServiceMonitor and PrometheusRule
kubectl apply -f k8s/advanced/monitoring/prometheus/

# Verify alerts are loaded
kubectl get prometheusrule -n monitoring
```

**Available Alerts:**
- High error rate (>5% for 5 minutes)
- High latency (95th percentile >1s)
- Service down
- High memory usage (>85%)
- Database pool exhaustion (>90%)
- High GC pause frequency

### Grafana Dashboards

**What it provides:**
- Visual monitoring of application metrics
- Historical trend analysis
- Performance troubleshooting
- Business metrics tracking

**Setup:**
```bash
# Import dashboard
kubectl create configmap springcrm-dashboard \
  --from-file=k8s/advanced/monitoring/grafana/springcrm-dashboard.json \
  -n monitoring

# Add labels for Grafana auto-discovery
kubectl label configmap springcrm-dashboard \
  grafana_dashboard=1 \
  -n monitoring
```

**Dashboard Panels:**
- Request rate and error rate
- Response time percentiles
- JVM memory and GC metrics
- Database connection pool status
- Pod status and current metrics

## 🔍 OpenTelemetry Observability

### What it provides:
- Distributed tracing across services
- Correlation between logs, metrics, and traces
- Performance bottleneck identification
- Service dependency mapping

### Prerequisites:
- OpenTelemetry Collector
- Jaeger or other tracing backend
- Application instrumentation

### Setup Steps:

1. **Create observability namespace:**
   ```bash
   kubectl create namespace observability
   ```

2. **Install Jaeger:**
   ```bash
   kubectl create -f https://github.com/jaegertracing/jaeger-operator/releases/download/v1.50.0/jaeger-operator.yaml -n observability
   
   # Create Jaeger instance
   kubectl apply -f - <<EOF
   apiVersion: jaegertracing.io/v1
   kind: Jaeger
   metadata:
     name: jaeger
     namespace: observability
   spec:
     strategy: production
     storage:
       type: memory
   EOF
   ```

3. **Deploy OpenTelemetry Collector:**
   ```bash
   kubectl apply -f k8s/advanced/observability/opentelemetry/
   ```

4. **Configure Spring Boot applications:**
   ```yaml
   # Add to application-k8s.yml
   management:
     tracing:
       sampling:
         probability: 0.1
     otlp:
       tracing:
         endpoint: http://otel-collector.observability.svc.cluster.local:4318/v1/traces
   ```

5. **Access Jaeger UI:**
   ```bash
   kubectl port-forward svc/jaeger-query -n observability 16686:16686
   # Open http://localhost:16686
   ```

## 🌐 Service Mesh (Advanced - Optional)

### Istio Setup

**What it provides:**
- Advanced traffic management
- Mutual TLS between services
- Circuit breakers and retries
- Observability out of the box

**Setup:**
```bash
# Install Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-*
export PATH=$PWD/bin:$PATH
istioctl install --set values.defaultRevision=default

# Enable sidecar injection
kubectl label namespace dev istio-injection=enabled
kubectl label namespace staging istio-injection=enabled
kubectl label namespace prod istio-injection=enabled

# Apply Istio configurations
kubectl apply -f k8s/advanced/service-mesh/istio/
```

## 🔧 Configuration Management

### Environment-Specific Features

Create overlay patches to enable/disable features per environment:

```yaml
# k8s/overlays/dev/advanced-features.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
- ../../base
- ../../advanced/hpa          # Enable HPA in dev
- ../../advanced/monitoring   # Enable monitoring in dev

patchesStrategicMerge:
- hpa-patches.yaml           # Reduce min replicas for dev
```

```yaml
# k8s/overlays/prod/advanced-features.yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
- ../../base
- ../../advanced/hpa
- ../../advanced/monitoring
- ../../advanced/argo-rollouts  # Enable rollouts in prod
- ../../advanced/observability # Enable full observability in prod
```

### Feature Flags

Use ConfigMaps to control feature behavior:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: feature-flags
data:
  ENABLE_CANARY_DEPLOYMENTS: "true"
  ENABLE_AUTO_SCALING: "true"
  ENABLE_DISTRIBUTED_TRACING: "true"
  TRACING_SAMPLE_RATE: "0.1"
  MAX_REPLICAS: "10"
  MIN_REPLICAS: "2"
```

## 📋 Implementation Checklist

### Phase 1: Basic Scaling (Low Risk)
- [ ] Install Metrics Server
- [ ] Apply HPA manifests
- [ ] Test scaling behavior
- [ ] Configure Pod Disruption Budgets
- [ ] Monitor resource usage

### Phase 2: Enhanced Monitoring (Low Risk)
- [ ] Apply Prometheus ServiceMonitor
- [ ] Configure alerting rules
- [ ] Import Grafana dashboards
- [ ] Test alert notifications
- [ ] Create runbook documentation

### Phase 3: Advanced Deployments (Medium Risk)
- [ ] Install Argo Rollouts controller
- [ ] Replace Deployments with Rollouts
- [ ] Configure canary analysis
- [ ] Test rollout and rollback
- [ ] Update CI/CD pipeline

### Phase 4: Observability (Medium Risk)
- [ ] Deploy OpenTelemetry Collector
- [ ] Install tracing backend (Jaeger)
- [ ] Instrument applications
- [ ] Configure trace sampling
- [ ] Verify end-to-end tracing

### Phase 5: Service Mesh (High Risk - Optional)
- [ ] Install Istio control plane
- [ ] Enable sidecar injection
- [ ] Configure traffic policies
- [ ] Implement security policies
- [ ] Monitor mesh performance

## 🚨 Rollback Procedures

### HPA Rollback:
```bash
kubectl delete hpa --all -n <namespace>
# Manually scale deployments if needed
kubectl scale deployment auth-service --replicas=2 -n dev
```

### Argo Rollouts Rollback:
```bash
kubectl argo rollouts abort auth-service -n dev
kubectl argo rollouts undo auth-service -n dev
# Or replace rollouts with standard deployments
kubectl apply -f k8s/base/auth-service/deployment.yaml
```

### Monitoring Rollback:
```bash
kubectl delete servicemonitor springcrm-services -n monitoring
kubectl delete prometheusrule springcrm-alerts -n monitoring
kubectl delete configmap springcrm-dashboard -n monitoring
```

### OpenTelemetry Rollback:
```bash
kubectl delete -f k8s/advanced/observability/opentelemetry/
# Remove tracing configuration from applications
```

## 🔍 Troubleshooting

### HPA Issues:
```bash
# Check metrics server
kubectl get apiservice v1beta1.metrics.k8s.io -o yaml

# Check HPA status
kubectl describe hpa auth-service-hpa -n dev

# Check resource usage
kubectl top pods -n dev
```

### Argo Rollouts Issues:
```bash
# Check rollout status
kubectl argo rollouts get rollout auth-service -n dev

# Check analysis runs
kubectl get analysisrun -n dev

# Check controller logs
kubectl logs -n argo-rollouts deployment/argo-rollouts-controller
```

### Monitoring Issues:
```bash
# Check ServiceMonitor discovery
kubectl get servicemonitor -n monitoring -o yaml

# Check Prometheus targets
kubectl port-forward svc/prometheus-server -n monitoring 9090:80
# Visit http://localhost:9090/targets

# Check alert status
# Visit http://localhost:9090/alerts
```

### OpenTelemetry Issues:
```bash
# Check collector status
kubectl logs deployment/otel-collector -n observability

# Check Jaeger connectivity
kubectl port-forward svc/jaeger-query -n observability 16686:16686

# Verify trace ingestion
# Check Jaeger UI for traces
```

## 📊 Performance Impact

### Resource Overhead:

| Component | CPU | Memory | Storage |
|-----------|-----|--------|---------|
| HPA | ~1m | ~10Mi | - |
| Prometheus | 100-500m | 1-4Gi | 10-50Gi |
| Grafana | 50-200m | 200-500Mi | 1-5Gi |
| Argo Rollouts | 50m | 128Mi | - |
| OpenTelemetry | 100-200m | 256-512Mi | - |
| Jaeger | 200-500m | 1-2Gi | 10-100Gi |

### Network Overhead:
- **Tracing:** ~1-5% additional network traffic
- **Metrics:** ~2-10MB/hour per service
- **Service Mesh:** ~5-15% latency increase

## 🔗 Related Documentation

- [Kubernetes GitOps Setup](./k8s-gitops-setup.md)
- [Spring Boot K8s Configuration](./spring-boot-k8s-config.md)
- [Argo Rollouts Documentation](https://argoproj.github.io/argo-rollouts/)
- [Prometheus Operator](https://prometheus-operator.dev/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)