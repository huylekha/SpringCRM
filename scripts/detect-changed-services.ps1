# PowerShell script to detect changed services based on git diff
# Usage: .\detect-changed-services.ps1 [-BaseRef "HEAD~1"] [-TargetRef "HEAD"] [-OutputFormat "json"]
# Example: .\detect-changed-services.ps1 -BaseRef "HEAD~1" -TargetRef "HEAD"
# Example: .\detect-changed-services.ps1 -BaseRef "origin/main" -TargetRef "HEAD" -OutputFormat "env"

param(
    [string]$BaseRef = "HEAD~1",
    [string]$TargetRef = "HEAD",
    [ValidateSet("json", "env", "text")]
    [string]$OutputFormat = "json"
)

Write-Host "🔍 Detecting changed services..." -ForegroundColor Yellow
Write-Host "   Base: $BaseRef" -ForegroundColor Gray
Write-Host "   Target: $TargetRef" -ForegroundColor Gray

# Get changed files
try {
    $ChangedFiles = git diff --name-only "$BaseRef..$TargetRef" 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Git diff failed"
    }
} catch {
    Write-Error "❌ Error: Unable to get git diff. Make sure both refs exist."
    exit 1
}

Write-Host "📁 Changed files:" -ForegroundColor Yellow
$ChangedFiles | ForEach-Object { Write-Host "     $_" -ForegroundColor Gray }

# Initialize flags
$ChangedAuth = $false
$ChangedCrm = $false
$ChangedGateway = $false
$ChangedFrontend = $false
$ChangedShared = $false

# Analyze changes
foreach ($file in $ChangedFiles) {
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
        '^k8s/' {
            # K8s changes don't trigger service rebuilds
        }
        '^docs/' {
            # Documentation changes don't trigger service rebuilds
        }
        '\.(md|txt|yml|yaml)$' {
            # Config/doc files at root don't trigger service rebuilds
            if ($file -match '^(docker-compose\.yml|\.github/workflows/)') {
                Write-Host "⚠️  Infrastructure file changed: $file" -ForegroundColor Yellow
            }
        }
    }
}

# If shared-lib changed, all backend services are affected
if ($ChangedShared) {
    Write-Host "🔄 Shared library changed - marking all backend services as affected" -ForegroundColor Yellow
    $ChangedAuth = $true
    $ChangedCrm = $true
    $ChangedGateway = $true
}

# Build affected services list
$AffectedServices = @()
if ($ChangedAuth) { $AffectedServices += "auth-service" }
if ($ChangedCrm) { $AffectedServices += "crm-service" }
if ($ChangedGateway) { $AffectedServices += "api-gateway" }
if ($ChangedFrontend) { $AffectedServices += "frontend" }

# Output results in requested format
switch ($OutputFormat) {
    "json" {
        $result = @{
            changed = @{
                "auth-service" = $ChangedAuth
                "crm-service" = $ChangedCrm
                "api-gateway" = $ChangedGateway
                "frontend" = $ChangedFrontend
                "shared-lib" = $ChangedShared
            }
            affected_services = $AffectedServices
            has_changes = $AffectedServices.Count -gt 0
        }
        $result | ConvertTo-Json -Depth 3
    }
    "env" {
        Write-Output "CHANGED_AUTH=$($ChangedAuth.ToString().ToLower())"
        Write-Output "CHANGED_CRM=$($ChangedCrm.ToString().ToLower())"
        Write-Output "CHANGED_GATEWAY=$($ChangedGateway.ToString().ToLower())"
        Write-Output "CHANGED_FRONTEND=$($ChangedFrontend.ToString().ToLower())"
        Write-Output "CHANGED_SHARED=$($ChangedShared.ToString().ToLower())"
        Write-Output "AFFECTED_SERVICES=$($AffectedServices -join ',')"
        Write-Output "HAS_CHANGES=$($($AffectedServices.Count -gt 0).ToString().ToLower())"
    }
    "text" {
        Write-Host "📊 Change detection results:" -ForegroundColor Cyan
        Write-Host "   auth-service: $($ChangedAuth.ToString().ToLower())" -ForegroundColor Gray
        Write-Host "   crm-service: $($ChangedCrm.ToString().ToLower())" -ForegroundColor Gray
        Write-Host "   api-gateway: $($ChangedGateway.ToString().ToLower())" -ForegroundColor Gray
        Write-Host "   frontend: $($ChangedFrontend.ToString().ToLower())" -ForegroundColor Gray
        Write-Host "   shared-lib: $($ChangedShared.ToString().ToLower())" -ForegroundColor Gray
        Write-Host ""
        
        if ($AffectedServices.Count -gt 0) {
            Write-Host "🎯 Affected services: $($AffectedServices -join ', ')" -ForegroundColor Green
        } else {
            Write-Host "✅ No services affected" -ForegroundColor Green
        }
        
        # Output for consumption by other tools
        Write-Output ($AffectedServices -join ' ')
    }
}