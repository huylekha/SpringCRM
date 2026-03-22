#!/bin/bash
set -euo pipefail

# GitOps update script - combines change detection and image tag updates
# Usage: ./gitops-update.sh <environment> <commit-sha> [base-ref]
# Example: ./gitops-update.sh dev abc123def
# Example: ./gitops-update.sh staging abc123def HEAD~1

ENVIRONMENT="${1:?Usage: gitops-update.sh <environment> <commit-sha> [base-ref]}"
COMMIT_SHA="${2:?Usage: gitops-update.sh <environment> <commit-sha> [base-ref]}"
BASE_REF="${3:-HEAD~1}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "🚀 GitOps Update Pipeline" >&2
echo "   Environment: $ENVIRONMENT" >&2
echo "   Commit SHA: $COMMIT_SHA" >&2
echo "   Base ref: $BASE_REF" >&2

# Step 1: Detect changed services
echo "" >&2
echo "🔍 Step 1: Detecting changed services..." >&2

if ! AFFECTED_SERVICES=$("$SCRIPT_DIR/detect-changed-services.sh" "$BASE_REF" "$COMMIT_SHA"); then
    echo "❌ Failed to detect changed services" >&2
    exit 1
fi

if [ -z "$AFFECTED_SERVICES" ]; then
    echo "ℹ️  No services affected by changes. Skipping image tag updates." >&2
    exit 0
fi

echo "🎯 Affected services: $AFFECTED_SERVICES" >&2

# Step 2: Update image tags for affected services
echo "" >&2
echo "🏷️  Step 2: Updating image tags..." >&2

# Convert space-separated string to array
read -ra SERVICES_ARRAY <<< "$AFFECTED_SERVICES"

if ! "$SCRIPT_DIR/update-kustomize-image-tags.sh" "$ENVIRONMENT" "$COMMIT_SHA" "${SERVICES_ARRAY[@]}"; then
    echo "❌ Failed to update image tags" >&2
    exit 1
fi

echo "" >&2
echo "✅ GitOps update completed successfully!" >&2
echo "   Environment: $ENVIRONMENT" >&2
echo "   Updated services: $AFFECTED_SERVICES" >&2
echo "   Commit SHA: $COMMIT_SHA" >&2