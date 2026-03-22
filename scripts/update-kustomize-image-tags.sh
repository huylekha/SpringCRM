#!/bin/bash
set -euo pipefail

# Script to update kustomize image tags and commit changes
# Usage: ./update-kustomize-image-tags.sh <environment> <commit-sha> [services...]
# Example: ./update-kustomize-image-tags.sh dev abc123def auth-service crm-service
# Example: ./update-kustomize-image-tags.sh staging abc123def  # Updates all services

ENVIRONMENT="${1:?Usage: update-kustomize-image-tags.sh <environment> <commit-sha> [services...]}"
COMMIT_SHA="${2:?Usage: update-kustomize-image-tags.sh <environment> <commit-sha> [services...]}"
SERVICES=("${@:3}")  # All remaining arguments are services

# Registry configuration
REGISTRY_URL="${REGISTRY_URL:-localhost:5000}"

# Validate environment
case "$ENVIRONMENT" in
    dev|staging|prod)
        ;;
    *)
        echo "❌ Error: Invalid environment '$ENVIRONMENT'. Must be: dev, staging, or prod" >&2
        exit 1
        ;;
esac

# Validate commit SHA format (basic check)
if [[ ! "$COMMIT_SHA" =~ ^[a-f0-9]{7,40}$ ]]; then
    echo "❌ Error: Invalid commit SHA format '$COMMIT_SHA'" >&2
    exit 1
fi

# Default services if none specified
if [ ${#SERVICES[@]} -eq 0 ]; then
    SERVICES=("auth-service" "crm-service" "api-gateway" "frontend")
    echo "📋 No services specified, updating all services: ${SERVICES[*]}" >&2
fi

OVERLAY_DIR="k8s/overlays/$ENVIRONMENT"
KUSTOMIZATION_FILE="$OVERLAY_DIR/kustomization.yaml"

# Check if overlay directory exists
if [ ! -d "$OVERLAY_DIR" ]; then
    echo "❌ Error: Overlay directory '$OVERLAY_DIR' does not exist" >&2
    exit 1
fi

# Check if kustomization file exists
if [ ! -f "$KUSTOMIZATION_FILE" ]; then
    echo "❌ Error: Kustomization file '$KUSTOMIZATION_FILE' does not exist" >&2
    exit 1
fi

echo "🏷️  Updating image tags for environment: $ENVIRONMENT" >&2
echo "   Commit SHA: $COMMIT_SHA" >&2
echo "   Services: ${SERVICES[*]}" >&2
echo "   Registry: $REGISTRY_URL" >&2

# Check if we have kustomize command
if ! command -v kustomize &> /dev/null; then
    echo "❌ Error: kustomize command not found. Please install kustomize." >&2
    echo "   Install: https://kustomize.io/" >&2
    exit 1
fi

# Backup original file
cp "$KUSTOMIZATION_FILE" "$KUSTOMIZATION_FILE.backup"

# Update each service image tag
UPDATED_SERVICES=()
for service in "${SERVICES[@]}"; do
    echo "🔄 Updating $service image tag..." >&2
    
    # Use kustomize to update the image tag
    if kustomize edit set image "$REGISTRY_URL/$service:$COMMIT_SHA" --file "$OVERLAY_DIR" 2>/dev/null; then
        UPDATED_SERVICES+=("$service")
        echo "   ✅ Updated $service to tag $COMMIT_SHA" >&2
    else
        echo "   ⚠️  Failed to update $service (image may not exist in kustomization)" >&2
    fi
done

# Check if any updates were made
if [ ${#UPDATED_SERVICES[@]} -eq 0 ]; then
    echo "❌ No services were updated. Restoring backup." >&2
    mv "$KUSTOMIZATION_FILE.backup" "$KUSTOMIZATION_FILE"
    exit 1
fi

# Remove backup
rm -f "$KUSTOMIZATION_FILE.backup"

echo "" >&2
echo "📝 Updated services: ${UPDATED_SERVICES[*]}" >&2

# Check if there are changes to commit
if git diff --quiet "$KUSTOMIZATION_FILE"; then
    echo "ℹ️  No changes detected in $KUSTOMIZATION_FILE" >&2
    exit 0
fi

# Show the diff
echo "📋 Changes made:" >&2
git diff "$KUSTOMIZATION_FILE" >&2

# Commit the changes
COMMIT_MESSAGE="chore($ENVIRONMENT): update image tags to $COMMIT_SHA

Updated services: ${UPDATED_SERVICES[*]}
Environment: $ENVIRONMENT
Commit SHA: $COMMIT_SHA

[skip ci]"

echo "" >&2
echo "💾 Committing changes..." >&2

# Configure git if needed (for CI environments)
if [ -z "${GIT_AUTHOR_NAME:-}" ]; then
    git config user.name "GitOps Bot"
    git config user.email "gitops-bot@springcrm.local"
fi

# Add and commit the changes
git add "$KUSTOMIZATION_FILE"
git commit -m "$COMMIT_MESSAGE"

# Get the new commit hash
NEW_COMMIT=$(git rev-parse HEAD)

echo "✅ Successfully committed changes" >&2
echo "   New commit: $NEW_COMMIT" >&2
echo "   File: $KUSTOMIZATION_FILE" >&2

# Output for consumption by CI/CD
echo "commit_sha=$NEW_COMMIT"
echo "updated_services=${UPDATED_SERVICES[*]}"
echo "environment=$ENVIRONMENT"