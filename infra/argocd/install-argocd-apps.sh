#!/bin/bash
set -euo pipefail

echo "🚀 Installing ArgoCD Applications for SpringCRM..."

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl is not installed or not in PATH"
    exit 1
fi

# Check if ArgoCD namespace exists
if ! kubectl get namespace argocd &> /dev/null; then
    echo "❌ ArgoCD namespace not found. Please install ArgoCD first."
    echo "   Run: kubectl create namespace argocd"
    echo "   Then: kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml"
    exit 1
fi

# Apply the AppProject first
echo "📋 Creating SpringCRM AppProject..."
kubectl apply -f projects/springcrm-project.yaml

# Wait a moment for the project to be created
sleep 2

# Apply the applications
echo "🏗️  Creating ArgoCD Applications..."
kubectl apply -f applications/argocd-app-dev.yaml
kubectl apply -f applications/argocd-app-staging.yaml
kubectl apply -f applications/argocd-app-prod.yaml

echo ""
echo "✅ ArgoCD Applications created successfully!"
echo ""
echo "📊 Check application status:"
echo "   kubectl get applications -n argocd"
echo ""
echo "🌐 Access ArgoCD UI:"
echo "   kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "   Then open: https://localhost:8080"
echo ""
echo "🔑 Get ArgoCD admin password:"
echo "   kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d"
echo ""
echo "🔄 Sync applications manually (if needed):"
echo "   argocd app sync springcrm-dev"
echo "   argocd app sync springcrm-staging" 
echo "   argocd app sync springcrm-prod"