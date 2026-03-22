# Monitoring Stack Setup Guide

This guide explains how to install and configure Prometheus + Grafana monitoring for the SpringCRM application using Helm and the existing monitoring configurations.

## Prerequisites

- Helm 3.x installed
- kubectl configured to access your local Kubernetes cluster
- At least 4GB of available memory in your cluster
- At least 2 CPU cores available

## Step 1: Install Helm (if not already installed)

### Windows (using Chocolatey)
```powershell
choco install kubernetes-helm
```

### Windows (using Scoop)
```powershell
scoop install helm
```

### macOS (using Homebrew)
```bash
brew install helm
```

### Linux
```bash
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
```

## Step 2: Add Prometheus Community Helm Repository

```bash
# Add the Prometheus community Helm repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

# Update Helm repositories
helm repo update

# Verify the repository was added
helm search repo prometheus-community/kube-prometheus-stack
```

## Step 3: Create Monitoring Namespace

```bash
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
```

## Step 4: Create Helm Values Configuration

Create a custom values file for the kube-prometheus-stack:

```bash
# Create values file for Prometheus + Grafana
cat > monitoring-values.yaml <<EOF
# Prometheus configuration
prometheus:
  prometheusSpec:
    # Resource limits for Prometheus
    resources:
      requests:
        memory: "1Gi"
        cpu: "500m"
      limits:
        memory: "2Gi"
        cpu: "1000m"
    
    # Storage configuration
    storageSpec:
      volumeClaimTemplate:
        spec:
          storageClassName: "standard"  # Use default storage class
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 10Gi
    
    # Retention policy
    retention: 30d
    retentionSize: 8GB
    
    # Service monitor selector
    serviceMonitorSelectorNilUsesHelmValues: false
    serviceMonitorSelector: {}
    
    # Rule selector
    ruleSelectorNilUsesHelmValues: false
    ruleSelector: {}

# Grafana configuration
grafana:
  # Enable Grafana
  enabled: true
  
  # Admin credentials
  adminPassword: "admin123"  # Change this in production!
  
  # Resource limits for Grafana
  resources:
    requests:
      memory: "256Mi"
      cpu: "100m"
    limits:
      memory: "512Mi"
      cpu: "200m"
  
  # Persistence
  persistence:
    enabled: true
    storageClassName: "standard"
    size: 5Gi
  
  # Service configuration
  service:
    type: ClusterIP
    port: 80
  
  # Grafana configuration
  grafana.ini:
    server:
      root_url: "http://localhost:3000"
    security:
      allow_embedding: true
    auth.anonymous:
      enabled: true
      org_role: Viewer
  
  # Default dashboards
  defaultDashboardsEnabled: true
  
  # Dashboard providers
  dashboardProviders:
    dashboardproviders.yaml:
      apiVersion: 1
      providers:
      - name: 'default'
        orgId: 1
        folder: ''
        type: file
        disableDeletion: false
        editable: true
        options:
          path: /var/lib/grafana/dashboards/default
      - name: 'springcrm'
        orgId: 1
        folder: 'SpringCRM'
        type: file
        disableDeletion: false
        editable: true
        options:
          path: /var/lib/grafana/dashboards/springcrm

# AlertManager configuration
alertmanager:
  alertmanagerSpec:
    resources:
      requests:
        memory: "128Mi"
        cpu: "50m"
      limits:
        memory: "256Mi"
        cpu: "100m"

# Node Exporter (for system metrics)
nodeExporter:
  enabled: true

# kube-state-metrics (for Kubernetes metrics)
kubeStateMetrics:
  enabled: true

# Disable components we don't need for local development
kubeApiServer:
  enabled: false
kubeControllerManager:
  enabled: false
kubeScheduler:
  enabled: false
kubeProxy:
  enabled: false
kubeEtcd:
  enabled: false
EOF
```

## Step 5: Install kube-prometheus-stack

```bash
# Install the monitoring stack
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --values monitoring-values.yaml \
  --wait

# Verify installation
kubectl get pods -n monitoring
kubectl get services -n monitoring
```

## Step 6: Apply SpringCRM ServiceMonitor and PrometheusRule

```bash
# Apply the ServiceMonitor and PrometheusRule from the repository
kubectl apply -f k8s/advanced/monitoring/prometheus/servicemonitor.yaml

# Verify ServiceMonitor was created
kubectl get servicemonitor -n monitoring
kubectl describe servicemonitor springcrm-services -n monitoring

# Verify PrometheusRule was created
kubectl get prometheusrule -n monitoring
kubectl describe prometheusrule springcrm-alerts -n monitoring
```

## Step 7: Import SpringCRM Grafana Dashboard

```bash
# Create ConfigMap from the dashboard JSON
kubectl create configmap springcrm-dashboard \
  --from-file=k8s/advanced/monitoring/grafana/springcrm-dashboard.json \
  --namespace=monitoring

# Label the ConfigMap for Grafana auto-discovery
kubectl label configmap springcrm-dashboard grafana_dashboard=1 -n monitoring

# Verify the ConfigMap was created
kubectl get configmap springcrm-dashboard -n monitoring
kubectl describe configmap springcrm-dashboard -n monitoring
```

## Step 8: Access Monitoring Services

### Access Grafana Dashboard

```bash
# Port-forward to Grafana
kubectl port-forward svc/prometheus-grafana -n monitoring 3000:80

# Open browser to http://localhost:3000
# Login: admin / admin123 (or the password you set)
```

### Access Prometheus UI

```bash
# Port-forward to Prometheus
kubectl port-forward svc/prometheus-kube-prometheus-prometheus -n monitoring 9090:9090

# Open browser to http://localhost:9090
```

### Access AlertManager UI

```bash
# Port-forward to AlertManager
kubectl port-forward svc/prometheus-kube-prometheus-alertmanager -n monitoring 9093:9093

# Open browser to http://localhost:9093
```

## Step 9: Verify Metrics Collection

### Check Prometheus Targets

1. Open Prometheus UI (http://localhost:9090)
2. Go to Status → Targets
3. Look for SpringCRM services in the targets list
4. Verify they are in "UP" state

### Check Grafana Dashboard

1. Open Grafana UI (http://localhost:3000)
2. Go to Dashboards → Browse
3. Look for "SpringCRM" folder
4. Open the SpringCRM dashboard
5. Verify metrics are being displayed

### Test Alerts

```bash
# Check if alert rules are loaded
kubectl exec -it prometheus-kube-prometheus-prometheus-0 -n monitoring -- \
  promtool query instant 'up{job=~".*springcrm.*"}'

# Check AlertManager for any active alerts
curl -s http://localhost:9093/api/v1/alerts | jq .
```

## Step 10: Configure Alert Notifications (Optional)

Create an AlertManager configuration for notifications:

```bash
# Create AlertManager config
cat > alertmanager-config.yaml <<EOF
apiVersion: v1
kind: Secret
metadata:
  name: alertmanager-kube-prometheus-alertmanager
  namespace: monitoring
type: Opaque
stringData:
  alertmanager.yml: |
    global:
      smtp_smarthost: 'localhost:587'
      smtp_from: 'alerts@springcrm.local'
    
    route:
      group_by: ['alertname']
      group_wait: 10s
      group_interval: 10s
      repeat_interval: 1h
      receiver: 'web.hook'
    
    receivers:
    - name: 'web.hook'
      webhook_configs:
      - url: 'http://localhost:5001/webhook'  # Replace with your webhook URL
        send_resolved: true
    
    # Email notifications (configure SMTP settings)
    # - name: 'email-notifications'
    #   email_configs:
    #   - to: 'admin@springcrm.local'
    #     subject: '[SpringCRM] Alert: {{ .GroupLabels.alertname }}'
    #     body: |
    #       {{ range .Alerts }}
    #       Alert: {{ .Annotations.summary }}
    #       Description: {{ .Annotations.description }}
    #       {{ end }}
EOF

# Apply the configuration
kubectl apply -f alertmanager-config.yaml

# Restart AlertManager to pick up new config
kubectl rollout restart statefulset/alertmanager-kube-prometheus-alertmanager -n monitoring
```

## Troubleshooting

### Prometheus Not Scraping SpringCRM Services

1. **Check ServiceMonitor selector:**
   ```bash
   kubectl get servicemonitor springcrm-services -n monitoring -o yaml
   ```

2. **Check service labels:**
   ```bash
   kubectl get services -n dev --show-labels | grep springcrm
   kubectl get services -n staging --show-labels | grep springcrm
   kubectl get services -n prod --show-labels | grep springcrm
   ```

3. **Verify service endpoints:**
   ```bash
   kubectl get endpoints -n dev
   ```

### Grafana Dashboard Not Loading

1. **Check ConfigMap:**
   ```bash
   kubectl get configmap springcrm-dashboard -n monitoring -o yaml
   ```

2. **Check Grafana logs:**
   ```bash
   kubectl logs deployment/prometheus-grafana -n monitoring
   ```

3. **Restart Grafana:**
   ```bash
   kubectl rollout restart deployment/prometheus-grafana -n monitoring
   ```

### High Resource Usage

1. **Check resource consumption:**
   ```bash
   kubectl top pods -n monitoring
   ```

2. **Reduce Prometheus retention:**
   ```bash
   helm upgrade prometheus prometheus-community/kube-prometheus-stack \
     --namespace monitoring \
     --set prometheus.prometheusSpec.retention=7d \
     --set prometheus.prometheusSpec.retentionSize=2GB
   ```

### Alerts Not Firing

1. **Check PrometheusRule:**
   ```bash
   kubectl get prometheusrule springcrm-alerts -n monitoring -o yaml
   ```

2. **Test alert expressions in Prometheus UI:**
   - Go to http://localhost:9090
   - Test the alert queries manually

3. **Check AlertManager configuration:**
   ```bash
   kubectl get secret alertmanager-kube-prometheus-alertmanager -n monitoring -o yaml
   ```

## Cleanup

To remove the monitoring stack:

```bash
# Delete SpringCRM monitoring resources
kubectl delete -f k8s/advanced/monitoring/prometheus/servicemonitor.yaml
kubectl delete configmap springcrm-dashboard -n monitoring

# Uninstall Helm release
helm uninstall prometheus -n monitoring

# Delete namespace (optional)
kubectl delete namespace monitoring

# Clean up values file
rm monitoring-values.yaml alertmanager-config.yaml
```

## Production Considerations

### Security
- Change default Grafana admin password
- Enable authentication and RBAC
- Use TLS for all connections
- Secure AlertManager webhook endpoints

### Performance
- Increase resource limits for high-traffic environments
- Configure appropriate retention policies
- Use remote storage for long-term metrics
- Enable horizontal scaling for Prometheus

### High Availability
- Run multiple Prometheus replicas
- Use external storage for Grafana dashboards
- Configure AlertManager clustering
- Set up cross-region monitoring

### Monitoring Best Practices
- Create custom dashboards for business metrics
- Set up proper alert routing and escalation
- Document runbooks for common alerts
- Regular backup of Grafana dashboards and Prometheus data

## Useful Commands

```bash
# Check monitoring stack status
kubectl get all -n monitoring

# View Prometheus configuration
kubectl get prometheus -n monitoring -o yaml

# Check ServiceMonitor discovery
kubectl get servicemonitor -n monitoring

# View Grafana datasources
kubectl exec -it deployment/prometheus-grafana -n monitoring -- \
  grafana-cli admin data-sources list

# Export Grafana dashboard
kubectl exec -it deployment/prometheus-grafana -n monitoring -- \
  curl -s http://admin:admin123@localhost:3000/api/dashboards/db/springcrm-dashboard

# Check AlertManager status
kubectl get alertmanager -n monitoring -o yaml
```