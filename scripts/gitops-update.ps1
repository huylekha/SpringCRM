# GitOps update script - combines change detection and image tag updates
# Usage: .\gitops-update.ps1 -Environment "dev" -CommitSha "abc123def" [-BaseRef "HEAD~1"]
# Example: .\gitops-update.ps1 -Environment "dev" -CommitSha "abc123def"
# Example: .\gitops-update.ps1 -Environment "staging" -CommitSha "abc123def" -BaseRef "HEAD~1"

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment,
    
    [Parameter(Mandatory=$true)]
    [string]$CommitSha,
    
    [string]$BaseRef = "HEAD~1"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "🚀 GitOps Update Pipeline" -ForegroundColor Green
Write-Host "   Environment: $Environment" -ForegroundColor Gray
Write-Host "   Commit SHA: $CommitSha" -ForegroundColor Gray
Write-Host "   Base ref: $BaseRef" -ForegroundColor Gray

# Step 1: Detect changed services
Write-Host ""
Write-Host "🔍 Step 1: Detecting changed services..." -ForegroundColor Yellow

try {
    $DetectionResult = & "$ScriptDir\detect-changed-services.ps1" -BaseRef $BaseRef -TargetRef $CommitSha -OutputFormat "text"
    if ($LASTEXITCODE -ne 0) {
        throw "Change detection failed"
    }
    
    $AffectedServices = $DetectionResult.Trim()
} catch {
    Write-Error "❌ Failed to detect changed services: $_"
    exit 1
}

if ([string]::IsNullOrEmpty($AffectedServices)) {
    Write-Host "ℹ️  No services affected by changes. Skipping image tag updates." -ForegroundColor Blue
    exit 0
}

Write-Host "🎯 Affected services: $AffectedServices" -ForegroundColor Green

# Step 2: Update image tags for affected services
Write-Host ""
Write-Host "🏷️  Step 2: Updating image tags..." -ForegroundColor Yellow

try {
    $ServicesArray = $AffectedServices -split ' ' | Where-Object { $_ -ne '' }
    $UpdateResult = & "$ScriptDir\update-kustomize-image-tags.ps1" -Environment $Environment -CommitSha $CommitSha -Services $ServicesArray
    if ($LASTEXITCODE -ne 0) {
        throw "Image tag update failed"
    }
} catch {
    Write-Error "❌ Failed to update image tags: $_"
    exit 1
}

Write-Host ""
Write-Host "✅ GitOps update completed successfully!" -ForegroundColor Green
Write-Host "   Environment: $Environment" -ForegroundColor Gray
Write-Host "   Updated services: $AffectedServices" -ForegroundColor Gray
Write-Host "   Commit SHA: $CommitSha" -ForegroundColor Gray