# PowerShell test script for change detection logic
param(
    [switch]$Verbose
)

Write-Host "🧪 Testing change detection logic..." -ForegroundColor Green

# Test function
function Test-Changes {
    param(
        [string]$Description,
        [string[]]$Files,
        [string[]]$Expected
    )
    
    Write-Host ""
    Write-Host "Test: $Description" -ForegroundColor Cyan
    if ($Verbose) {
        Write-Host "Files: $($Files -join ', ')" -ForegroundColor Gray
    }
    
    # Simulate the change detection logic
    $ChangedAuth = $false
    $ChangedCrm = $false
    $ChangedGateway = $false
    $ChangedFrontend = $false
    $ChangedShared = $false
    
    foreach ($file in $Files) {
        switch -Regex ($file) {
            '^backend/shared-lib/' {
                $ChangedShared = $true
            }
            '^backend/auth-service/' {
                $ChangedAuth = $true
            }
            '^backend/crm-service/' {
                $ChangedCrm = $true
            }
            '^backend/api-gateway/' {
                $ChangedGateway = $true
            }
            '^frontend/' {
                $ChangedFrontend = $true
            }
        }
    }
    
    # If shared-lib changed, all backend services are affected
    if ($ChangedShared) {
        $ChangedAuth = $true
        $ChangedCrm = $true
        $ChangedGateway = $true
    }
    
    # Build result
    $AffectedServices = @()
    if ($ChangedAuth) { $AffectedServices += "auth-service" }
    if ($ChangedCrm) { $AffectedServices += "crm-service" }
    if ($ChangedGateway) { $AffectedServices += "api-gateway" }
    if ($ChangedFrontend) { $AffectedServices += "frontend" }
    
    $Result = $AffectedServices -join " "
    $ExpectedStr = $Expected -join " "
    
    if ($Verbose) {
        Write-Host "Expected: '$ExpectedStr'" -ForegroundColor Yellow
        Write-Host "Got: '$Result'" -ForegroundColor Yellow
    }
    
    if ($Result -eq $ExpectedStr) {
        Write-Host "✅ PASS" -ForegroundColor Green
        return $true
    } else {
        Write-Host "❌ FAIL - Expected '$ExpectedStr', got '$Result'" -ForegroundColor Red
        return $false
    }
}

# Run tests
Write-Host "Running change detection tests..." -ForegroundColor Yellow

$TestResults = @()

$TestResults += Test-Changes -Description "Single auth service change" `
    -Files @("backend/auth-service/src/main/java/AuthController.java") `
    -Expected @("auth-service")

$TestResults += Test-Changes -Description "Single CRM service change" `
    -Files @("backend/crm-service/src/main/java/CrmController.java") `
    -Expected @("crm-service")

$TestResults += Test-Changes -Description "Frontend change" `
    -Files @("frontend/src/components/Dashboard.tsx") `
    -Expected @("frontend")

$TestResults += Test-Changes -Description "Shared library change (affects all backend)" `
    -Files @("backend/shared-lib/src/main/java/SharedUtil.java") `
    -Expected @("auth-service", "crm-service", "api-gateway")

$TestResults += Test-Changes -Description "Multiple service changes" `
    -Files @("backend/auth-service/src/main/java/AuthController.java", "frontend/src/components/Login.tsx") `
    -Expected @("auth-service", "frontend")

$TestResults += Test-Changes -Description "Documentation change (no services affected)" `
    -Files @("docs/README.md") `
    -Expected @()

$TestResults += Test-Changes -Description "K8s manifest change (no services affected)" `
    -Files @("k8s/overlays/dev/configmap-patches.yaml") `
    -Expected @()

$TestResults += Test-Changes -Description "Mixed changes including shared-lib" `
    -Files @("backend/shared-lib/pom.xml", "frontend/package.json", "docs/api.md") `
    -Expected @("auth-service", "crm-service", "api-gateway", "frontend")

# Summary
Write-Host ""
$PassedTests = ($TestResults | Where-Object { $_ -eq $true }).Count
$TotalTests = $TestResults.Count

if ($PassedTests -eq $TotalTests) {
    Write-Host "🎉 All $TotalTests tests passed!" -ForegroundColor Green
} else {
    Write-Host "❌ $($TotalTests - $PassedTests) out of $TotalTests tests failed!" -ForegroundColor Red
    exit 1
}