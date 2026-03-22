#!/bin/bash
set -euo pipefail

# Test script for change detection logic
echo "🧪 Testing change detection logic..."

# Test function
test_changes() {
    local description="$1"
    local files="$2"
    local expected="$3"
    
    echo ""
    echo "Test: $description"
    echo "Files: $files"
    
    # Create a temporary git commit with the test files
    # (This is a simulation - in real usage, files would already be changed)
    
    # For now, we'll test the logic directly by simulating the file patterns
    local changed_auth=false
    local changed_crm=false
    local changed_gateway=false
    local changed_frontend=false
    local changed_shared=false
    
    # Simulate the change detection logic
    while IFS= read -r file; do
        case "$file" in
            backend/shared-lib/*)
                changed_shared=true
                ;;
            backend/auth-service/*)
                changed_auth=true
                ;;
            backend/crm-service/*)
                changed_crm=true
                ;;
            backend/api-gateway/*)
                changed_gateway=true
                ;;
            frontend/*)
                changed_frontend=true
                ;;
        esac
    done <<< "$files"
    
    # If shared-lib changed, all backend services are affected
    if [ "$changed_shared" = "true" ]; then
        changed_auth=true
        changed_crm=true
        changed_gateway=true
    fi
    
    # Build result
    local affected_services=()
    if [ "$changed_auth" = "true" ]; then
        affected_services+=("auth-service")
    fi
    if [ "$changed_crm" = "true" ]; then
        affected_services+=("crm-service")
    fi
    if [ "$changed_gateway" = "true" ]; then
        affected_services+=("api-gateway")
    fi
    if [ "$changed_frontend" = "true" ]; then
        affected_services+=("frontend")
    fi
    
    local result="${affected_services[*]}"
    
    echo "Expected: $expected"
    echo "Got: $result"
    
    if [ "$result" = "$expected" ]; then
        echo "✅ PASS"
    else
        echo "❌ FAIL"
        return 1
    fi
}

# Run tests
echo "Running change detection tests..."

test_changes \
    "Single auth service change" \
    "backend/auth-service/src/main/java/AuthController.java" \
    "auth-service"

test_changes \
    "Single CRM service change" \
    "backend/crm-service/src/main/java/CrmController.java" \
    "crm-service"

test_changes \
    "Frontend change" \
    "frontend/src/components/Dashboard.tsx" \
    "frontend"

test_changes \
    "Shared library change (affects all backend)" \
    "backend/shared-lib/src/main/java/SharedUtil.java" \
    "auth-service crm-service api-gateway"

test_changes \
    "Multiple service changes" \
    $'backend/auth-service/src/main/java/AuthController.java\nfrontend/src/components/Login.tsx' \
    "auth-service frontend"

test_changes \
    "Documentation change (no services affected)" \
    "docs/README.md" \
    ""

test_changes \
    "K8s manifest change (no services affected)" \
    "k8s/overlays/dev/configmap-patches.yaml" \
    ""

test_changes \
    "Mixed changes including shared-lib" \
    $'backend/shared-lib/pom.xml\nfrontend/package.json\ndocs/api.md' \
    "auth-service crm-service api-gateway frontend"

echo ""
echo "🎉 All tests completed!"