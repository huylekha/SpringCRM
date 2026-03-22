# Kubernetes GitOps Setup Guide

This comprehensive guide walks you through setting up the complete GitOps CI/CD system for SpringCRM on a local Kubernetes cluster.

## 🎯 Overview

**What you'll build:**
- Local Kubernetes cluster (Docker Desktop + kind)
- Local Docker registry for images
- NGINX Ingress Controller for routing
- ArgoCD for GitOps deployment
- Complete CI/CD pipeline with GitHub Actions
- Monitoring with Prometheus & Grafana (optional)

**Architecture:**
```
GitHub → Actions → Docker Build → Local Registry → ArgoCD → Kubernetes
   ↓         ↓           ↓            ↓          ↓         ↓
 Code    Tests     Image Tags    GitOps     Sync    Rolling Update
```

## 📋 Prerequisites

### Required Software

- **Docker Desktop** (with Kubernetes enabled) OR **kind**
- **kubectl** (Kubernetes CLI)
- **kustomize** (for manifest management)
- **Git** (for repository operations)
- **Node.js 20+** (for frontend builds)
- **Java 21** (for backend builds)
- **Maven 3.9+** (for backend builds)

### System Requirements

- **RAM:** 8GB minimum, 16GB recommended
- **CPU:** 4 cores minimum
- **Disk:** 20GB free space
- **OS:** Windows 10/11, macOS, or Linux

### Verification Commands

```bash
# Check prerequisites
docker --version                 # Docker 24.0+
kubectl version --client         # kubectl 1.28+
kustomize version               # kustomize 5.0+
git --version                   # Git 2.30+
node --version                  # Node 20+
java --version                  # Java 21
mvn --version                   # Maven 3.9+
```

## 🚀 Step 1: Setup Local Kubernetes Cluster

### Option A: Docker Desktop Kubernetes

1. **Enable Kubernetes in Docker Desktop:**
   - Open Docker Desktop Settings
   - Go to Kubernetes tab
   - Check "Enable Kubernetes"
   - Click "Apply & Restart"

2. **Verify cluster:**
   ```bash
   kubectl cluster-info
   kubectl get nodes
   ```

### Option B: kind (Kubernetes in Docker)

1. **Install kind:**
   ```bash
   # Windows (using Chocolatey)
   choco install kind
   
   # macOS (using Homebrew)
   brew install kind
   
   # Linux
   curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
   chmod +x ./kind
   sudo mv ./kind /usr/local/bin/kind
   ```

2. **Create cluster with custom configuration:**
   ```bash
   # Create kind-config.yaml
   cat <<EOF > kind-config.yaml
   kind: Cluster
   apiVersion: kind.x-k8s.io/v1alpha4
   nodes:
   - role: control-plane
     kubeadmConfigPatches:
     - |
       kind: InitConfiguration
       nodeRegistration:
         kubeletExtraArgs:
           node-labels: "ingress-ready=true"
     extraPortMappings:
     - containerPort: 80
       hostPort: 80
       protocol: TCP
     - containerPort: 443
       hostPort: 443
       protocol: TCP
   - role: worker
   - role: worker
   EOF
   
   # Create cluster
   kind create cluster --config kind-config.yaml --name springcrm
   
   # Set context
   kubectl cluster-info --context kind-springcrm
   ```

## 🐳 Step 2: Setup Local Docker Registry

### Create Registry

```bash
# Create local registry
docker run -d \
  --restart=always \
  --name registry \
  -p 5000:5000 \
  -v registry-data:/var/lib/registry \
  registry:2

# Verify registry is running
curl http://localhost:5000/v2/_catalog
```

### Configure kind for Local Registry (if using kind)

```bash
# Connect registry to kind network
docker network connect kind registry

# Create registry config for kind
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-registry-hosting
  namespace: kube-public
data:
  localRegistryHosting.v1: |
    host: "localhost:5000"
    help: "https://kind.sigs.k8s.io/docs/user/local-registry/"
EOF
```

### Test Registry

```bash
# Pull, tag, and push a test image
docker pull hello-world
docker tag hello-world localhost:5000/hello-world
docker push localhost:5000/hello-world

# Verify
curl http://localhost:5000/v2/_catalog
```

## 🌐 Step 3: Install NGINX Ingress Controller

### Install via kubectl

```bash
# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml

# Wait for deployment
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s

# Verify installation
kubectl get pods -n ingress-nginx
kubectl get services -n ingress-nginx
```

### Test Ingress (Optional)

```bash
# Create test deployment
kubectl create deployment hello-world --image=gcr.io/google-samples/hello-app:1.0
kubectl expose deployment hello-world --port=8080

# Create test ingress
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: hello-world-ingress
  annotations:
    kubernetes.io/ingress.class: nginx
spec:
  rules:
  - host: localhost
    http:
      paths:
      - path: /hello
        pathType: Prefix
        backend:
          service:
            name: hello-world
            port:
              number: 8080
EOF

# Test (wait a moment for ingress to be ready)
curl http://localhost/hello

# Cleanup test resources
kubectl delete ingress hello-world-ingress
kubectl delete service hello-world
kubectl delete deployment hello-world
```

## 🔄 Step 4: Install ArgoCD

### Install ArgoCD

```bash
# Create ArgoCD namespace
kubectl create namespace argocd

# Install ArgoCD
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Wait for ArgoCD to be ready
kubectl wait --for=condition=available --timeout=300s deployment/argocd-server -n argocd

# Verify installation
kubectl get pods -n argocd
```

### Access ArgoCD UI

```bash
# Get initial admin password
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d

# Port forward to access UI
kubectl port-forward svc/argocd-server -n argocd 8080:443

# Open browser to https://localhost:8080
# Username: admin
# Password: (from command above)
```

### Install ArgoCD CLI (Optional)

```bash
# Windows (using Chocolatey)
choco install argocd-cli

# macOS (using Homebrew)
brew install argocd

# Linux
curl -sSL -o argocd-linux-amd64 https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
sudo install -m 555 argocd-linux-amd64 /usr/local/bin/argocd
rm argocd-linux-amd64

# Login via CLI
argocd login localhost:8080 --username admin --password <password> --insecure
```

## 📦 Step 5: Install kustomize

### Install kustomize

```bash
# Windows (using Chocolatey)
choco install kustomize

# macOS (using Homebrew)  
brew install kustomize

# Linux
curl -s "https://raw.githubusercontent.com/kubernetes-sigs/kustomize/master/hack/install_kustomize.sh" | bash
sudo mv kustomize /usr/local/bin/

# Verify installation
kustomize version
```

## 🏗️ Step 6: Deploy SpringCRM Applications

### Create Namespaces

```bash
# Create application namespaces
kubectl create namespace dev
kubectl create namespace staging  
kubectl create namespace prod

# Label namespaces
kubectl label namespace dev environment=dev
kubectl label namespace staging environment=staging
kubectl label namespace prod environment=prod
```

### Deploy ArgoCD Applications

```bash
# Navigate to project root
cd /path/to/SpringCRM

# Install ArgoCD applications
cd infra/argocd

# Linux/Mac
chmod +x install-argocd-apps.sh
./install-argocd-apps.sh

# Windows
.\install-argocd-apps.ps1

# Verify applications
kubectl get applications -n argocd
```

### Initial Deployment (Manual)

Since CI/CD isn't set up yet, do an initial manual deployment:

```bash
# Build and push initial images
cd /path/to/SpringCRM

# Build backend services
docker build --context backend --file backend/auth-service/Dockerfile --tag localhost:5000/auth-service:initial .
docker build --context backend --file backend/crm-service/Dockerfile --tag localhost:5000/crm-service:initial .
docker build --context backend --file backend/api-gateway/Dockerfile --tag localhost:5000/api-gateway:initial .

# Build frontend
docker build --context frontend --file frontend/Dockerfile --tag localhost:5000/frontend:initial .

# Push images
docker push localhost:5000/auth-service:initial
docker push localhost:5000/crm-service:initial
docker push localhost:5000/api-gateway:initial
docker push localhost:5000/frontend:initial

# Update kustomize overlays with initial tags
cd k8s/overlays/dev
kustomize edit set image localhost:5000/auth-service:initial
kustomize edit set image localhost:5000/crm-service:initial
kustomize edit set image localhost:5000/api-gateway:initial
kustomize edit set image localhost:5000/frontend:initial

# Apply manually for initial deployment
kustomize build . | kubectl apply -f -
```

## 🔐 Step 7: Configure Secrets Management

### Option A: Plain Secrets (Development Only)

**⚠️ Warning:** Only use for local development. Never commit real secrets to Git.

```bash
# Create development secrets
kubectl create secret generic auth-service-secret -n dev \
  --from-literal=DB_PASSWORD=dev_password \
  --from-literal=JWT_SECRET=dev-jwt-secret-key-for-development-must-be-at-least-64-chars-long \
  --from-literal=SENTRY_DSN=""

kubectl create secret generic crm-service-secret -n dev \
  --from-literal=DB_PASSWORD=dev_password \
  --from-literal=SENTRY_DSN=""

kubectl create secret generic postgres-secret -n dev \
  --from-literal=POSTGRES_PASSWORD=dev_password

kubectl create secret generic frontend-secret -n dev \
  --from-literal=NEXTAUTH_SECRET=dev-nextauth-secret-key-for-development-only-32chars \
  --from-literal=SENTRY_DSN=""
```

### Option B: Sealed Secrets (Recommended)

```bash
# Install Sealed Secrets Controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Install kubeseal CLI
# Windows (using Chocolatey)
choco install kubeseal

# macOS (using Homebrew)
brew install kubeseal

# Linux
wget https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/kubeseal-0.24.0-linux-amd64.tar.gz
tar -xvzf kubeseal-0.24.0-linux-amd64.tar.gz kubeseal
sudo install -m 755 kubeseal /usr/local/bin/kubeseal

# Create sealed secret
echo -n 'your-secret-password' | kubectl create secret generic auth-service-secret \
  --dry-run=client --from-file=DB_PASSWORD=/dev/stdin -o yaml | \
  kubeseal -o yaml > auth-service-sealed-secret.yaml

# Apply sealed secret
kubectl apply -f auth-service-sealed-secret.yaml -n dev
```

### Option C: External Secrets Operator

```bash
# Install External Secrets Operator
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets -n external-secrets-system --create-namespace

# Configure with your secret management system (HashiCorp Vault, AWS Secrets Manager, etc.)
```

## 🔧 Step 8: Configure GitHub Actions

### Setup GitHub Secrets

In your GitHub repository, go to Settings → Secrets and variables → Actions:

```bash
# Required secrets
GITOPS_TOKEN=<GitHub Personal Access Token with repo permissions>

# Optional secrets (for notifications)
SLACK_WEBHOOK_URL=<Slack webhook URL>
TEAMS_WEBHOOK_URL=<Teams webhook URL>
```

### Configure Self-Hosted Runner (if needed)

```bash
# Navigate to your repository
# Go to Settings → Actions → Runners → New self-hosted runner
# Follow the setup instructions

# For Windows, use the PowerShell script provided in the repo:
cd actions-runner
.\install-service.ps1
```

### Test GitHub Actions

```bash
# Make a test commit to trigger the workflow
git checkout -b test-gitops
echo "# Test GitOps" >> README.md
git add README.md
git commit -m "test: trigger GitOps workflow"
git push origin test-gitops

# Check workflow in GitHub Actions tab
```

## 🧪 Step 9: Run Smoke Tests

### Test Application Health

```bash
# Wait for all pods to be ready
kubectl wait --for=condition=ready pod --all -n dev --timeout=300s

# Check pod status
kubectl get pods -n dev

# Test health endpoints
curl http://localhost/dev/auth-service/actuator/health
curl http://localhost/dev/crm-service/actuator/health
curl http://localhost/dev/api-gateway/actuator/health
curl http://localhost/dev/frontend/api/health
```

### Test API Gateway Routing

```bash
# Test auth service through gateway
curl http://localhost/dev/api-gateway/api/v1/auth/actuator/health

# Test CRM service through gateway
curl http://localhost/dev/api-gateway/api/v1/crm/actuator/health
```

### Test Frontend

```bash
# Test frontend directly
curl http://localhost/dev/frontend/

# Test frontend API health
curl http://localhost/dev/frontend/api/health
```

### Test Database Connectivity

```bash
# Connect to PostgreSQL pod
kubectl exec -it deployment/dev-postgres -n dev -- psql -U crm_user -d crm_platform_dev

# Run test query
SELECT version();
\q
```

### Test Redis Connectivity

```bash
# Connect to Redis pod
kubectl exec -it deployment/dev-redis -n dev -- redis-cli

# Test Redis
ping
set test-key "test-value"
get test-key
exit
```

## 📊 Step 10: Setup Monitoring (Optional)

### Install Prometheus & Grafana

```bash
# Add Helm repositories
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Create monitoring namespace
kubectl create namespace monitoring

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace monitoring \
  --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
  --set prometheus.prometheusSpec.podMonitorSelectorNilUsesHelmValues=false

# Wait for deployment
kubectl wait --for=condition=ready pod --all -n monitoring --timeout=300s

# Access Grafana
kubectl port-forward svc/prometheus-grafana -n monitoring 3000:80

# Grafana credentials:
# Username: admin
# Password: prom-operator (default)
```

### Configure ServiceMonitor for SpringCRM

```yaml
# Create servicemonitor.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: springcrm-metrics
  namespace: monitoring
  labels:
    app: springcrm
spec:
  selector:
    matchLabels:
      app.kubernetes.io/part-of: springcrm
  endpoints:
  - port: http
    path: /actuator/prometheus
    interval: 30s
  namespaceSelector:
    matchNames:
    - dev
    - staging
    - prod
```

```bash
# Apply ServiceMonitor
kubectl apply -f servicemonitor.yaml
```

## 🔍 Step 11: Verification & Testing

### Complete System Test

```bash
# 1. Make a code change
echo "console.log('GitOps test');" >> frontend/src/app/page.tsx

# 2. Commit and push
git add .
git commit -m "feat: test GitOps deployment"
git push origin develop

# 3. Watch GitHub Actions
# Go to GitHub Actions tab and monitor the workflow

# 4. Watch ArgoCD sync
kubectl get applications -n argocd -w

# 5. Verify deployment
kubectl get pods -n dev -w

# 6. Test the change
curl http://localhost/dev/frontend/
```

### Performance Test

```bash
# Install hey (HTTP load testing tool)
# Windows
choco install hey

# macOS
brew install hey

# Linux
wget https://hey-release.s3.us-east-2.amazonaws.com/hey_linux_amd64
chmod +x hey_linux_amd64
sudo mv hey_linux_amd64 /usr/local/bin/hey

# Run load test
hey -n 1000 -c 10 http://localhost/dev/api-gateway/actuator/health
```

### Chaos Testing (Optional)

```bash
# Kill a pod and watch it recover
kubectl delete pod -l app=auth-service -n dev

# Watch recovery
kubectl get pods -n dev -w

# Verify service availability during recovery
while true; do curl -s http://localhost/dev/auth-service/actuator/health && echo " - OK" || echo " - FAIL"; sleep 1; done
```

## 🛠️ Troubleshooting

### Common Issues

#### 1. Pods Stuck in Pending

```bash
# Check node resources
kubectl describe nodes

# Check pod events
kubectl describe pod <pod-name> -n <namespace>

# Check resource requests/limits
kubectl get pods -n <namespace> -o yaml | grep -A 5 resources
```

#### 2. Image Pull Errors

```bash
# Check if registry is accessible
curl http://localhost:5000/v2/_catalog

# Check image exists
curl http://localhost:5000/v2/<service-name>/tags/list

# Check pod image pull policy
kubectl describe pod <pod-name> -n <namespace>
```

#### 3. Ingress Not Working

```bash
# Check ingress controller
kubectl get pods -n ingress-nginx

# Check ingress resource
kubectl describe ingress -n <namespace>

# Check service endpoints
kubectl get endpoints -n <namespace>
```

#### 4. ArgoCD Sync Issues

```bash
# Check application status
kubectl describe application <app-name> -n argocd

# Force refresh
argocd app refresh <app-name>

# Check ArgoCD logs
kubectl logs deployment/argocd-application-controller -n argocd
```

#### 5. Database Connection Issues

```bash
# Check PostgreSQL pod
kubectl logs deployment/dev-postgres -n dev

# Test connection from app pod
kubectl exec -it deployment/dev-auth-service -n dev -- nc -zv postgres 5432

# Check network policies
kubectl get networkpolicy -n dev
```

### Debug Commands

```bash
# Get all resources in namespace
kubectl get all -n dev

# Describe problematic pod
kubectl describe pod <pod-name> -n dev

# Get pod logs
kubectl logs <pod-name> -n dev --tail=100 -f

# Execute into pod
kubectl exec -it <pod-name> -n dev -- /bin/sh

# Check resource usage
kubectl top nodes
kubectl top pods -n dev

# Check events
kubectl get events -n dev --sort-by='.lastTimestamp'
```

## 📋 Final Checklist

### Infrastructure ✅

- [ ] Kubernetes cluster running
- [ ] Local Docker registry accessible
- [ ] NGINX Ingress Controller installed
- [ ] ArgoCD installed and accessible
- [ ] kustomize installed

### Applications ✅

- [ ] All namespaces created (dev, staging, prod)
- [ ] ArgoCD applications deployed
- [ ] All pods running and ready
- [ ] Health checks passing
- [ ] Ingress routing working

### CI/CD ✅

- [ ] GitHub Actions workflow configured
- [ ] Self-hosted runner connected (if used)
- [ ] Secrets configured
- [ ] Test deployment successful
- [ ] GitOps flow working (code → CI → ArgoCD → K8s)

### Security ✅

- [ ] Secrets management configured
- [ ] Network policies applied (if needed)
- [ ] RBAC configured
- [ ] No plain-text secrets in Git

### Monitoring ✅

- [ ] Prometheus installed (optional)
- [ ] Grafana accessible (optional)
- [ ] ServiceMonitor configured (optional)
- [ ] Metrics collection working (optional)

### Testing ✅

- [ ] Smoke tests passing
- [ ] Load tests completed
- [ ] Chaos testing validated (optional)
- [ ] End-to-end flow verified

## 🎉 Success!

You now have a complete GitOps CI/CD system running locally! 

**Next Steps:**
- Customize the configuration for your specific needs
- Set up production-grade secrets management
- Configure monitoring and alerting
- Implement additional security measures
- Scale to cloud environments when ready

## 🔗 Related Documentation

- [Docker Build Templates](./docker-build-templates.md)
- [Spring Boot K8s Configuration](./spring-boot-k8s-config.md)
- [GitOps Scripts Documentation](../../scripts/README.md)
- [ArgoCD Applications](../../infra/argocd/README.md)