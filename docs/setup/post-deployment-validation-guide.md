# Post-Deployment Validation Guide

This guide provides comprehensive validation steps to verify that all components of the SpringCRM GitOps CI/CD system are working correctly after deployment.

## Prerequisites

- kubectl configured and connected to your cluster
- ArgoCD CLI installed (optional but recommended)
- curl or similar HTTP client
- Access to Prometheus and Grafana UIs

## Step 1: ArgoCD Application Sync Validation

### Check ArgoCD Application Status

```bash
# List all SpringCRM applications
kubectl get applications -n argocd | grep springcrm

# Check detailed status for each environment
kubectl get application springcrm-dev -n argocd -o yaml
kubectl get application springcrm-staging -n argocd -o yaml
kubectl get application springcrm-prod -n argocd -o yaml
```

### Verify Sync Status

```bash
# Check if applications are synced and healthy
kubectl get applications -n argocd -o custom-columns="NAME:.metadata.name,SYNC:.status.sync.status,HEALTH:.status.health.status,REVISION:.status.sync.revision"

# Expected output:
# NAME               SYNC     HEALTH   REVISION
# springcrm-dev      Synced   Healthy  abc123...
# springcrm-staging  Synced   Healthy  abc123...
# springcrm-prod     Synced   Healthy  abc123...
```

### Manual ArgoCD Sync (if needed)

```bash
# Force sync if applications are out of sync
kubectl patch application springcrm-dev -n argocd --type='merge' -p='{"operation":{"sync":{"syncStrategy":{"hook":{"force":true}}}}}'

# Or using ArgoCD CLI (if installed)
argocd app sync springcrm-dev
argocd app sync springcrm-staging
argocd app sync springcrm-prod
```

### Check ArgoCD Logs

```bash
# Check ArgoCD application controller logs
kubectl logs -n argocd deployment/argocd-application-controller -f --tail=50

# Look for sync operations and any errors
kubectl logs -n argocd deployment/argocd-server -f --tail=50 | grep -i error
```

## Step 2: Pod and Service Health Validation

### Check Pod Status Across All Environments

```bash
# Check dev environment
echo "=== DEV ENVIRONMENT ==="
kubectl get pods -n dev -o wide
kubectl get services -n dev
echo ""

# Check staging environment
echo "=== STAGING ENVIRONMENT ==="
kubectl get pods -n staging -o wide
kubectl get services -n staging
echo ""

# Check production environment
echo "=== PRODUCTION ENVIRONMENT ==="
kubectl get pods -n prod -o wide
kubectl get services -n prod
echo ""
```

### Verify All Pods are Running

```bash
# Check for any pods not in Running state
kubectl get pods --all-namespaces | grep -v Running | grep -v Completed

# Check pod readiness and liveness
kubectl get pods -n dev -o custom-columns="NAME:.metadata.name,READY:.status.containerStatuses[0].ready,RESTARTS:.status.containerStatuses[0].restartCount"
kubectl get pods -n staging -o custom-columns="NAME:.metadata.name,READY:.status.containerStatuses[0].ready,RESTARTS:.status.containerStatuses[0].restartCount"
kubectl get pods -n prod -o custom-columns="NAME:.metadata.name,READY:.status.containerStatuses[0].ready,RESTARTS:.status.containerStatuses[0].restartCount"
```

### Check Pod Logs for Errors

```bash
# Function to check logs for all services in an environment
check_logs() {
    local env=$1
    echo "=== Checking logs for $env environment ==="
    
    for service in auth-service crm-service api-gateway frontend postgres redis; do
        echo "--- $service logs ---"
        kubectl logs -n $env deployment/$env-$service --tail=10 2>/dev/null || echo "No deployment found for $env-$service"
        echo ""
    done
}

# Check logs for all environments
check_logs "dev"
check_logs "staging"
check_logs "prod"
```

## Step 3: Flyway Migration Validation

### Check Flyway Migration Success

```bash
# Check auth-service Flyway logs
kubectl logs -n dev deployment/dev-auth-service | grep -i flyway
kubectl logs -n staging deployment/staging-auth-service | grep -i flyway
kubectl logs -n prod deployment/prod-auth-service | grep -i flyway

# Check crm-service Flyway logs
kubectl logs -n dev deployment/dev-crm-service | grep -i flyway
kubectl logs -n staging deployment/staging-crm-service | grep -i flyway
kubectl logs -n prod deployment/prod-crm-service | grep -i flyway
```

### Verify Database Schema

```bash
# Connect to PostgreSQL and verify tables exist
verify_database_schema() {
    local env=$1
    echo "=== Verifying database schema for $env ==="
    
    # Get PostgreSQL pod
    local postgres_pod=$(kubectl get pods -n $env -l app=postgres -o jsonpath='{.items[0].metadata.name}')
    
    if [ ! -z "$postgres_pod" ]; then
        echo "Connecting to PostgreSQL pod: $postgres_pod"
        
        # Check if tables exist
        kubectl exec -n $env $postgres_pod -- psql -U crm_user -d crm_platform_${env} -c "
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'public' 
        ORDER BY table_name;
        "
        
        # Check Flyway schema history
        kubectl exec -n $env $postgres_pod -- psql -U crm_user -d crm_platform_${env} -c "
        SELECT version, description, success, installed_on 
        FROM flyway_schema_history 
        ORDER BY installed_rank;
        "
    else
        echo "PostgreSQL pod not found in $env environment"
    fi
}

# Verify schema for all environments
verify_database_schema "dev"
verify_database_schema "staging"
verify_database_schema "prod"
```

### Check for Migration Errors

```bash
# Look for Flyway errors in application logs
kubectl logs -n dev deployment/dev-auth-service | grep -i "flyway\|migration\|schema" | grep -i error
kubectl logs -n dev deployment/dev-crm-service | grep -i "flyway\|migration\|schema" | grep -i error

# Check if shared messaging tables exist
check_messaging_tables() {
    local env=$1
    local postgres_pod=$(kubectl get pods -n $env -l app=postgres -o jsonpath='{.items[0].metadata.name}')
    
    if [ ! -z "$postgres_pod" ]; then
        echo "Checking messaging tables in $env:"
        kubectl exec -n $env $postgres_pod -- psql -U crm_user -d crm_platform_${env} -c "
        SELECT 'idempotency_records' as table_name, count(*) as row_count FROM idempotency_records
        UNION ALL
        SELECT 'inbox_messages', count(*) FROM inbox_messages
        UNION ALL
        SELECT 'outbox_messages', count(*) FROM outbox_messages;
        "
    fi
}

check_messaging_tables "dev"
check_messaging_tables "staging"
check_messaging_tables "prod"
```

## Step 4: Ingress and Health Endpoint Validation

### Test Ingress Routing

```bash
# Function to test health endpoints
test_health_endpoints() {
    local env=$1
    local base_url="http://springcrm.com"
    
    echo "=== Testing health endpoints for $env environment ==="
    
    # Test auth-service
    echo "Testing auth-service..."
    curl -s -o /dev/null -w "Status: %{http_code}\n" "$base_url/$env/auth-service/actuator/health" || echo "Failed to connect"
    
    # Test crm-service
    echo "Testing crm-service..."
    curl -s -o /dev/null -w "Status: %{http_code}\n" "$base_url/$env/crm-service/actuator/health" || echo "Failed to connect"
    
    # Test api-gateway
    echo "Testing api-gateway..."
    curl -s -o /dev/null -w "Status: %{http_code}\n" "$base_url/$env/api-gateway/actuator/health" || echo "Failed to connect"
    
    # Test frontend
    echo "Testing frontend..."
    curl -s -o /dev/null -w "Status: %{http_code}\n" "$base_url/$env/frontend/api/health" || echo "Failed to connect"
    
    echo ""
}

# Test all environments
test_health_endpoints "dev"
test_health_endpoints "staging"
test_health_endpoints "prod"
```

### Detailed Health Check

```bash
# Get detailed health information
get_detailed_health() {
    local env=$1
    local service=$2
    local endpoint="actuator/health"
    
    if [ "$service" = "frontend" ]; then
        endpoint="api/health"
    fi
    
    echo "=== Detailed health for $env/$service ==="
    curl -s "http://localhost/$env/$service/$endpoint" | jq . 2>/dev/null || curl -s "http://localhost/$env/$service/$endpoint"
    echo ""
}

# Get detailed health for key services
get_detailed_health "dev" "auth-service"
get_detailed_health "dev" "crm-service"
get_detailed_health "dev" "api-gateway"
```

### Test TLS in Production

```bash
# Test HTTPS endpoints in production (if TLS is configured)
echo "=== Testing production HTTPS endpoints ==="

# Test with self-signed certificate (ignore certificate errors)
curl -k -s -o /dev/null -w "HTTPS Status: %{http_code}\n" "https://springcrm.com/prod/auth-service/actuator/health" || echo "HTTPS failed"
curl -k -s -o /dev/null -w "HTTPS Status: %{http_code}\n" "https://springcrm.com/prod/frontend/api/health" || echo "HTTPS failed"

# Check certificate details
echo "Certificate information:"
openssl s_client -connect springcrm.com:443 -servername springcrm.com </dev/null 2>/dev/null | openssl x509 -noout -text | grep -A 2 "Subject:"
```

## Step 5: Monitoring and Metrics Validation

### Check Prometheus Targets

```bash
# Port-forward to Prometheus (run in background)
kubectl port-forward -n monitoring svc/prometheus-kube-prometheus-prometheus 9090:9090 &
PROMETHEUS_PID=$!

# Wait for port-forward to establish
sleep 5

# Check Prometheus targets
echo "=== Checking Prometheus targets ==="
curl -s "http://localhost:9090/api/v1/targets" | jq '.data.activeTargets[] | select(.labels.job | contains("springcrm")) | {job: .labels.job, health: .health, lastScrape: .lastScrape}'

# Check if SpringCRM services are being scraped
curl -s "http://localhost:9090/api/v1/query?query=up{job=~\".*springcrm.*\"}" | jq '.data.result[] | {job: .metric.job, instance: .metric.instance, value: .value[1]}'

# Kill port-forward
kill $PROMETHEUS_PID 2>/dev/null
```

### Verify ServiceMonitor Configuration

```bash
# Check ServiceMonitor exists and is configured correctly
kubectl get servicemonitor -n monitoring springcrm-services -o yaml

# Verify service labels match ServiceMonitor selector
kubectl get services -n dev --show-labels | grep springcrm
kubectl get services -n staging --show-labels | grep springcrm
kubectl get services -n prod --show-labels | grep springcrm
```

### Test Grafana Dashboard

```bash
# Port-forward to Grafana
kubectl port-forward -n monitoring svc/prometheus-grafana 3000:80 &
GRAFANA_PID=$!

# Wait for port-forward
sleep 5

echo "=== Grafana Dashboard Test ==="
echo "1. Open http://localhost:3000 in your browser"
echo "2. Login with admin/admin123 (or your configured password)"
echo "3. Navigate to Dashboards > Browse > SpringCRM folder"
echo "4. Open the SpringCRM dashboard"
echo "5. Verify metrics are displaying correctly"

# Check if dashboard ConfigMap exists
kubectl get configmap springcrm-dashboard -n monitoring

# Kill port-forward
kill $GRAFANA_PID 2>/dev/null
```

### Verify Alert Rules

```bash
# Check PrometheusRule exists
kubectl get prometheusrule -n monitoring springcrm-alerts -o yaml

# Test alert expressions (requires Prometheus port-forward)
kubectl port-forward -n monitoring svc/prometheus-kube-prometheus-prometheus 9090:9090 &
PROMETHEUS_PID=$!
sleep 5

# Test some alert queries
echo "=== Testing alert queries ==="
curl -s "http://localhost:9090/api/v1/query?query=up{job=~\".*springcrm.*\"}" | jq '.data.result | length'
curl -s "http://localhost:9090/api/v1/query?query=rate(http_server_requests_total{job=~\".*springcrm.*\"}[5m])" | jq '.data.result | length'

kill $PROMETHEUS_PID 2>/dev/null
```

## Step 6: Secret Values Validation

### Verify Secrets Are Not Overwritten

```bash
# Check that manually created secrets still exist and have correct data
check_secret_integrity() {
    local env=$1
    echo "=== Checking secret integrity for $env environment ==="
    
    # List all secrets
    kubectl get secrets -n $env
    
    # Check specific secrets exist
    for secret in auth-service-secret crm-service-secret postgres-secret frontend-secret; do
        if kubectl get secret $secret -n $env >/dev/null 2>&1; then
            echo "✅ $secret exists"
            # Check if it has the expected keys (don't show values)
            kubectl get secret $secret -n $env -o jsonpath='{.data}' | jq -r 'keys[]' | sed 's/^/  - /'
        else
            echo "❌ $secret missing"
        fi
    done
    echo ""
}

# Check all environments
check_secret_integrity "dev"
check_secret_integrity "staging"
check_secret_integrity "prod"
```

### Verify ArgoCD Ignores Secret Differences

```bash
# Check ArgoCD application configuration for ignoreDifferences
echo "=== Verifying ArgoCD ignoreDifferences configuration ==="

kubectl get application springcrm-prod -n argocd -o jsonpath='{.spec.ignoreDifferences}' | jq .
kubectl get application springcrm-staging -n argocd -o jsonpath='{.spec.ignoreDifferences}' | jq .

# Check if there are any sync differences related to secrets
kubectl get application springcrm-prod -n argocd -o jsonpath='{.status.sync.comparedTo.destination.namespace}'
kubectl get application springcrm-staging -n argocd -o jsonpath='{.status.sync.comparedTo.destination.namespace}'
```

### Test Secret Values in Pods

```bash
# Verify pods can access secret values (without exposing them)
test_secret_access() {
    local env=$1
    echo "=== Testing secret access in $env environment ==="
    
    # Test auth-service can access DB_PASSWORD
    local auth_pod=$(kubectl get pods -n $env -l app=auth-service -o jsonpath='{.items[0].metadata.name}')
    if [ ! -z "$auth_pod" ]; then
        echo "Testing auth-service secret access..."
        kubectl exec -n $env $auth_pod -- sh -c 'echo "DB_PASSWORD length: ${#DB_PASSWORD}"' 2>/dev/null || echo "Failed to access DB_PASSWORD"
        kubectl exec -n $env $auth_pod -- sh -c 'echo "JWT_SECRET length: ${#JWT_SECRET}"' 2>/dev/null || echo "Failed to access JWT_SECRET"
    fi
    
    # Test PostgreSQL can access POSTGRES_PASSWORD
    local postgres_pod=$(kubectl get pods -n $env -l app=postgres -o jsonpath='{.items[0].metadata.name}')
    if [ ! -z "$postgres_pod" ]; then
        echo "Testing PostgreSQL secret access..."
        kubectl exec -n $env $postgres_pod -- sh -c 'echo "POSTGRES_PASSWORD length: ${#POSTGRES_PASSWORD}"' 2>/dev/null || echo "Failed to access POSTGRES_PASSWORD"
    fi
    
    echo ""
}

# Test secret access for all environments
test_secret_access "dev"
test_secret_access "staging" 
test_secret_access "prod"
```

## Step 7: End-to-End Smoke Tests

### API Functionality Tests

```bash
# Test API endpoints
test_api_functionality() {
    local env=$1
    local base_url="http://springcrm.com/$env"
    
    echo "=== API functionality tests for $env ==="
    
    # Test API Gateway routing
    echo "Testing API Gateway routing..."
    curl -s -o /dev/null -w "API Gateway: %{http_code}\n" "$base_url/api-gateway/actuator/info"
    
    # Test auth service endpoints
    echo "Testing auth service..."
    curl -s -o /dev/null -w "Auth Info: %{http_code}\n" "$base_url/auth-service/actuator/info"
    
    # Test CRM service endpoints
    echo "Testing CRM service..."
    curl -s -o /dev/null -w "CRM Info: %{http_code}\n" "$base_url/crm-service/actuator/info"
    
    # Test frontend
    echo "Testing frontend..."
    curl -s -o /dev/null -w "Frontend: %{http_code}\n" "$base_url/frontend/"
    
    echo ""
}

# Test all environments
test_api_functionality "dev"
test_api_functionality "staging"
test_api_functionality "prod"
```

### Database Connectivity Tests

```bash
# Test database connectivity from services
test_database_connectivity() {
    local env=$1
    echo "=== Database connectivity tests for $env ==="
    
    # Check auth-service database connection
    local auth_pod=$(kubectl get pods -n $env -l app=auth-service -o jsonpath='{.items[0].metadata.name}')
    if [ ! -z "$auth_pod" ]; then
        echo "Testing auth-service database connectivity..."
        kubectl logs -n $env $auth_pod --tail=50 | grep -i "database\|connection\|hikari" | tail -3
    fi
    
    # Check crm-service database connection
    local crm_pod=$(kubectl get pods -n $env -l app=crm-service -o jsonpath='{.items[0].metadata.name}')
    if [ ! -z "$crm_pod" ]; then
        echo "Testing crm-service database connectivity..."
        kubectl logs -n $env $crm_pod --tail=50 | grep -i "database\|connection\|hikari" | tail -3
    fi
    
    echo ""
}

# Test database connectivity for all environments
test_database_connectivity "dev"
test_database_connectivity "staging"
test_database_connectivity "prod"
```

## Step 8: Performance and Resource Validation

### Check Resource Usage

```bash
# Check resource usage across all environments
echo "=== Resource usage validation ==="

kubectl top pods -n dev 2>/dev/null || echo "Metrics server not available for dev"
kubectl top pods -n staging 2>/dev/null || echo "Metrics server not available for staging"
kubectl top pods -n prod 2>/dev/null || echo "Metrics server not available for prod"

# Check resource limits and requests
kubectl get pods -n dev -o custom-columns="NAME:.metadata.name,CPU-REQ:.spec.containers[0].resources.requests.cpu,MEM-REQ:.spec.containers[0].resources.requests.memory,CPU-LIM:.spec.containers[0].resources.limits.cpu,MEM-LIM:.spec.containers[0].resources.limits.memory"
```

### Check Storage Usage

```bash
# Check PVC usage
echo "=== Storage validation ==="
kubectl get pvc --all-namespaces
kubectl get pv

# Check if PostgreSQL data is persistent
check_postgresql_data() {
    local env=$1
    local postgres_pod=$(kubectl get pods -n $env -l app=postgres -o jsonpath='{.items[0].metadata.name}')
    
    if [ ! -z "$postgres_pod" ]; then
        echo "PostgreSQL data in $env:"
        kubectl exec -n $env $postgres_pod -- df -h /var/lib/postgresql/data
    fi
}

check_postgresql_data "dev"
check_postgresql_data "staging"
check_postgresql_data "prod"
```

## Step 9: Validation Summary Report

### Generate Validation Report

```bash
# Create a comprehensive validation report
generate_validation_report() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    local report_file="validation-report-$(date '+%Y%m%d-%H%M%S').txt"
    
    echo "SpringCRM GitOps Validation Report" > $report_file
    echo "Generated: $timestamp" >> $report_file
    echo "=================================" >> $report_file
    echo "" >> $report_file
    
    # ArgoCD Applications
    echo "ArgoCD Applications:" >> $report_file
    kubectl get applications -n argocd -o custom-columns="NAME:.metadata.name,SYNC:.status.sync.status,HEALTH:.status.health.status" | grep springcrm >> $report_file
    echo "" >> $report_file
    
    # Pod Status
    echo "Pod Status Summary:" >> $report_file
    for env in dev staging prod; do
        echo "$env environment:" >> $report_file
        kubectl get pods -n $env --no-headers | awk '{print "  " $1 " - " $3}' >> $report_file 2>/dev/null || echo "  No pods found" >> $report_file
    done
    echo "" >> $report_file
    
    # Health Endpoints
    echo "Health Endpoint Status:" >> $report_file
    for env in dev staging prod; do
        echo "$env environment:" >> $report_file
        for service in auth-service crm-service api-gateway; do
            status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost/$env/$service/actuator/health" 2>/dev/null || echo "FAIL")
            echo "  $service: $status" >> $report_file
        done
        # Frontend
        status=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost/$env/frontend/api/health" 2>/dev/null || echo "FAIL")
        echo "  frontend: $status" >> $report_file
    done
    echo "" >> $report_file
    
    # Secrets
    echo "Secret Status:" >> $report_file
    for env in dev staging prod; do
        echo "$env environment:" >> $report_file
        for secret in auth-service-secret crm-service-secret postgres-secret frontend-secret; do
            if kubectl get secret $secret -n $env >/dev/null 2>&1; then
                echo "  $secret: EXISTS" >> $report_file
            else
                echo "  $secret: MISSING" >> $report_file
            fi
        done
    done
    echo "" >> $report_file
    
    # Monitoring
    echo "Monitoring Status:" >> $report_file
    if kubectl get servicemonitor springcrm-services -n monitoring >/dev/null 2>&1; then
        echo "  ServiceMonitor: EXISTS" >> $report_file
    else
        echo "  ServiceMonitor: MISSING" >> $report_file
    fi
    
    if kubectl get configmap springcrm-dashboard -n monitoring >/dev/null 2>&1; then
        echo "  Grafana Dashboard: EXISTS" >> $report_file
    else
        echo "  Grafana Dashboard: MISSING" >> $report_file
    fi
    
    echo "" >> $report_file
    echo "Validation completed at $timestamp" >> $report_file
    
    echo "Validation report generated: $report_file"
    cat $report_file
}

# Generate the report
generate_validation_report
```

## Troubleshooting Common Issues

### ArgoCD Sync Issues

```bash
# Force refresh ArgoCD application
kubectl patch application springcrm-dev -n argocd -p='{"metadata":{"annotations":{"argocd.argoproj.io/refresh":"hard"}}}' --type=merge

# Check ArgoCD application events
kubectl describe application springcrm-dev -n argocd | grep Events -A 20
```

### Pod Startup Issues

```bash
# Check pod events
kubectl describe pod POD_NAME -n NAMESPACE | grep Events -A 20

# Check init containers
kubectl get pods -n dev -o custom-columns="NAME:.metadata.name,INIT:.status.initContainerStatuses[0].ready"
```

### Network Connectivity Issues

```bash
# Test internal service connectivity
kubectl run test-pod --image=busybox --rm -it -- nslookup dev-auth-service.dev.svc.cluster.local

# Check ingress controller
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx deployment/ingress-nginx-controller --tail=50
```

## Conclusion

This validation guide ensures:
- ✅ ArgoCD applications are synced and healthy
- ✅ All pods are running and ready
- ✅ Flyway migrations completed successfully
- ✅ Health endpoints are accessible via ingress
- ✅ Monitoring is collecting metrics
- ✅ Secrets are properly managed and not overwritten
- ✅ End-to-end functionality is working

Run these validations after any deployment or configuration change to ensure system integrity.