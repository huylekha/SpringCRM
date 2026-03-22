#!/bin/bash
set -euo pipefail

# Script to detect changed services based on git diff
# Usage: ./detect-changed-services.sh [base-ref] [target-ref]
# Example: ./detect-changed-services.sh HEAD~1 HEAD
# Example: ./detect-changed-services.sh origin/main HEAD

BASE_REF="${1:-HEAD~1}"
TARGET_REF="${2:-HEAD}"
OUTPUT_FORMAT="${OUTPUT_FORMAT:-json}"  # json, env, or text

echo "🔍 Detecting changed services..." >&2
echo "   Base: $BASE_REF" >&2
echo "   Target: $TARGET_REF" >&2

# Get changed files
if ! CHANGED_FILES=$(git diff --name-only "$BASE_REF..$TARGET_REF" 2>/dev/null); then
    echo "❌ Error: Unable to get git diff. Make sure both refs exist." >&2
    exit 1
fi

echo "📁 Changed files:" >&2
echo "$CHANGED_FILES" | sed 's/^/     /' >&2

# Initialize flags
CHANGED_AUTH=false
CHANGED_CRM=false
CHANGED_GATEWAY=false
CHANGED_FRONTEND=false
CHANGED_SHARED=false

# Analyze changes
while IFS= read -r file; do
    case "$file" in
        backend/shared-lib/*)
            CHANGED_SHARED=true
            ;;
        backend/auth-service/*)
            CHANGED_AUTH=true
            ;;
        backend/crm-service/*)
            CHANGED_CRM=true
            ;;
        backend/api-gateway/*)
            CHANGED_GATEWAY=true
            ;;
        frontend/*)
            CHANGED_FRONTEND=true
            ;;
        k8s/*)
            # K8s changes don't trigger service rebuilds
            ;;
        docs/*)
            # Documentation changes don't trigger service rebuilds
            ;;
        *.md|*.txt|*.yml|*.yaml)
            # Config/doc files at root don't trigger service rebuilds
            case "$file" in
                docker-compose.yml|.github/workflows/*)
                    # These might affect all services
                    echo "⚠️  Infrastructure file changed: $file" >&2
                    ;;
            esac
            ;;
    esac
done <<< "$CHANGED_FILES"

# If shared-lib changed, all backend services are affected
if [ "$CHANGED_SHARED" = "true" ]; then
    echo "🔄 Shared library changed - marking all backend services as affected" >&2
    CHANGED_AUTH=true
    CHANGED_CRM=true
    CHANGED_GATEWAY=true
fi

# Build affected services list
AFFECTED_SERVICES=()
if [ "$CHANGED_AUTH" = "true" ]; then
    AFFECTED_SERVICES+=("auth-service")
fi
if [ "$CHANGED_CRM" = "true" ]; then
    AFFECTED_SERVICES+=("crm-service")
fi
if [ "$CHANGED_GATEWAY" = "true" ]; then
    AFFECTED_SERVICES+=("api-gateway")
fi
if [ "$CHANGED_FRONTEND" = "true" ]; then
    AFFECTED_SERVICES+=("frontend")
fi

# Output results in requested format
case "$OUTPUT_FORMAT" in
    json)
        cat <<EOF
{
  "changed": {
    "auth-service": $CHANGED_AUTH,
    "crm-service": $CHANGED_CRM,
    "api-gateway": $CHANGED_GATEWAY,
    "frontend": $CHANGED_FRONTEND,
    "shared-lib": $CHANGED_SHARED
  },
  "affected_services": [$(IFS=,; echo "${AFFECTED_SERVICES[*]/#/\"}" | sed 's/,/","/g' | sed 's/"$//')],
  "has_changes": $([ ${#AFFECTED_SERVICES[@]} -gt 0 ] && echo "true" || echo "false")
}
EOF
        ;;
    env)
        echo "CHANGED_AUTH=$CHANGED_AUTH"
        echo "CHANGED_CRM=$CHANGED_CRM"
        echo "CHANGED_GATEWAY=$CHANGED_GATEWAY"
        echo "CHANGED_FRONTEND=$CHANGED_FRONTEND"
        echo "CHANGED_SHARED=$CHANGED_SHARED"
        echo "AFFECTED_SERVICES=$(IFS=,; echo "${AFFECTED_SERVICES[*]}")"
        echo "HAS_CHANGES=$([ ${#AFFECTED_SERVICES[@]} -gt 0 ] && echo "true" || echo "false")"
        ;;
    text|*)
        echo "📊 Change detection results:" >&2
        echo "   auth-service: $CHANGED_AUTH" >&2
        echo "   crm-service: $CHANGED_CRM" >&2
        echo "   api-gateway: $CHANGED_GATEWAY" >&2
        echo "   frontend: $CHANGED_FRONTEND" >&2
        echo "   shared-lib: $CHANGED_SHARED" >&2
        echo "" >&2
        if [ ${#AFFECTED_SERVICES[@]} -gt 0 ]; then
            echo "🎯 Affected services: ${AFFECTED_SERVICES[*]}" >&2
        else
            echo "✅ No services affected" >&2
        fi
        
        # Output for consumption by other tools
        echo "${AFFECTED_SERVICES[*]}"
        ;;
esac