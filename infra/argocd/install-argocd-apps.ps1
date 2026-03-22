# PowerShell script to install ArgoCD Applications for SpringCRM
param(
    [switch]$Force
)

Write-Host "🚀 Installing ArgoCD Applications for SpringCRM..." -ForegroundColor Green

# Check if kubectl is available
try {
    kubectl version --client --short | Out-Null
} catch {
    Write-Error "❌ kubectl is not installed or not in PATH"
    exit 1
}

# Check if ArgoCD namespace exists
try {
    kubectl get namespace argocd | Out-Null
} catch {
    Write-Error "❌ ArgoCD namespace not found. Please install ArgoCD first."
    Write-Host "   Run: kubectl create namespace argocd" -ForegroundColor Yellow
    Write-Host "   Then: kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml" -ForegroundColor Yellow
    exit 1
}

# Apply the AppProject first
Write-Host "📋 Creating SpringCRM AppProject..." -ForegroundColor Yellow
kubectl apply -f projects/springcrm-project.yaml

# Wait a moment for the project to be created
Start-Sleep 2

# Apply the applications
Write-Host "🏗️  Creating ArgoCD Applications..." -ForegroundColor Yellow
kubectl apply -f applications/argocd-app-dev.yaml
kubectl apply -f applications/argocd-app-staging.yaml
kubectl apply -f applications/argocd-app-prod.yaml

Write-Host ""
Write-Host "✅ ArgoCD Applications created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📊 Check application status:" -ForegroundColor Cyan
Write-Host "   kubectl get applications -n argocd" -ForegroundColor White
Write-Host ""
Write-Host "🌐 Access ArgoCD UI:" -ForegroundColor Cyan
Write-Host "   kubectl port-forward svc/argocd-server -n argocd 8080:443" -ForegroundColor White
Write-Host "   Then open: https://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "🔑 Get ArgoCD admin password:" -ForegroundColor Cyan
Write-Host "   kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d" -ForegroundColor White
Write-Host ""
Write-Host "🔄 Sync applications manually (if needed):" -ForegroundColor Cyan
Write-Host "   argocd app sync springcrm-dev" -ForegroundColor White
Write-Host "   argocd app sync springcrm-staging" -ForegroundColor White
Write-Host "   argocd app sync springcrm-prod" -ForegroundColor White