#!/usr/bin/env sh

# Service Detection Script
# Detects which backend services and frontend have changes
# Sets environment variables for selective testing

# Get list of staged files
STAGED_FILES=$(git diff --cached --name-only)

echo "🔍 Detecting changed services..."

# Initialize flags
CHANGED_SHARED_LIB=false
CHANGED_AUTH_SERVICE=false
CHANGED_CRM_SERVICE=false
CHANGED_API_GATEWAY=false
CHANGED_FRONTEND=false

# Check each staged file
for file in $STAGED_FILES; do
  case "$file" in
    backend/shared-lib/*)
      CHANGED_SHARED_LIB=true
      ;;
    backend/auth-service/*)
      CHANGED_AUTH_SERVICE=true
      ;;
    backend/crm-service/*)
      CHANGED_CRM_SERVICE=true
      ;;
    backend/api-gateway/*)
      CHANGED_API_GATEWAY=true
      ;;
    frontend/*)
      CHANGED_FRONTEND=true
      ;;
  esac
done

# If shared-lib changed, all backend services are affected
if [ "$CHANGED_SHARED_LIB" = true ]; then
  echo "   📦 shared-lib changed → Testing all backend services"
  CHANGED_AUTH_SERVICE=true
  CHANGED_CRM_SERVICE=true
  CHANGED_API_GATEWAY=true
fi

# Report detected changes
if [ "$CHANGED_AUTH_SERVICE" = true ]; then
  echo "   ✓ auth-service"
fi
if [ "$CHANGED_CRM_SERVICE" = true ]; then
  echo "   ✓ crm-service"
fi
if [ "$CHANGED_API_GATEWAY" = true ]; then
  echo "   ✓ api-gateway"
fi
if [ "$CHANGED_FRONTEND" = true ]; then
  echo "   ✓ frontend"
fi

# Export flags for use in pre-commit hook
export CHANGED_SHARED_LIB
export CHANGED_AUTH_SERVICE
export CHANGED_CRM_SERVICE
export CHANGED_API_GATEWAY
export CHANGED_FRONTEND
