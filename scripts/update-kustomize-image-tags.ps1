# PowerShell script to update kustomize image tags and commit changes
# Usage: .\update-kustomize-image-tags.ps1 -Environment "dev" -CommitSha "abc123def" [-Services @("auth-service", "crm-service")]
# Example: .\update-kustomize-image-tags.ps1 -Environment "dev" -CommitSha "abc123def" -Services @("auth-service", "crm-service")
# Example: .\update-kustomize-image-tags.ps1 -Environment "staging" -CommitSha "abc123def"  # Updates all services

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("dev", "staging", "prod")]
    [string]$Environment,
    
    [Parameter(Mandatory=$true)]
    [ValidatePattern("^[a-f0-9]{7,40}$")]
    [string]$CommitSha,
    
    [string[]]$Services = @("auth-service", "crm-service", "api-gateway", "frontend"),
    
    [string]$RegistryUrl = "localhost:5000"
)

Write-Host "🏷️  Updating image tags for environment: $Environment" -ForegroundColor Green
Write-Host "   Commit SHA: $CommitSha" -ForegroundColor Gray
Write-Host "   Services: $($Services -join ', ')" -ForegroundColor Gray
Write-Host "   Registry: $RegistryUrl" -ForegroundColor Gray

$OverlayDir = "k8s/overlays/$Environment"
$KustomizationFile = "$OverlayDir/kustomization.yaml"

# Check if overlay directory exists
if (!(Test-Path $OverlayDir -PathType Container)) {
    Write-Error "❌ Error: Overlay directory '$OverlayDir' does not exist"
    exit 1
}

# Check if kustomization file exists
if (!(Test-Path $KustomizationFile -PathType Leaf)) {
    Write-Error "❌ Error: Kustomization file '$KustomizationFile' does not exist"
    exit 1
}

# Check if we have kustomize command
try {
    kustomize version | Out-Null
} catch {
    Write-Error "❌ Error: kustomize command not found. Please install kustomize."
    Write-Host "   Install: https://kustomize.io/" -ForegroundColor Yellow
    exit 1
}

# Backup original file
Copy-Item $KustomizationFile "$KustomizationFile.backup"

# Update each service image tag
$UpdatedServices = @()
foreach ($service in $Services) {
    Write-Host "🔄 Updating $service image tag..." -ForegroundColor Yellow
    
    # Use kustomize to update the image tag
    $originalLocation = Get-Location
    try {
        Set-Location $OverlayDir
        $result = kustomize edit set image "$RegistryUrl/${service}:$CommitSha" 2>$null
        if ($LASTEXITCODE -eq 0) {
            $UpdatedServices += $service
            Write-Host "   ✅ Updated $service to tag $CommitSha" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️  Failed to update $service (image may not exist in kustomization)" -ForegroundColor Yellow
        }
    } finally {
        Set-Location $originalLocation
    }
}

# Check if any updates were made
if ($UpdatedServices.Count -eq 0) {
    Write-Host "❌ No services were updated. Restoring backup." -ForegroundColor Red
    Move-Item "$KustomizationFile.backup" $KustomizationFile -Force
    exit 1
}

# Remove backup
Remove-Item "$KustomizationFile.backup" -Force

Write-Host ""
Write-Host "📝 Updated services: $($UpdatedServices -join ', ')" -ForegroundColor Green

# Check if there are changes to commit
$gitStatus = git status --porcelain $KustomizationFile
if ([string]::IsNullOrEmpty($gitStatus)) {
    Write-Host "ℹ️  No changes detected in $KustomizationFile" -ForegroundColor Blue
    exit 0
}

# Show the diff
Write-Host "📋 Changes made:" -ForegroundColor Cyan
git diff $KustomizationFile

# Commit the changes
$CommitMessage = @"
chore($Environment): update image tags to $CommitSha

Updated services: $($UpdatedServices -join ', ')
Environment: $Environment
Commit SHA: $CommitSha

[skip ci]
"@

Write-Host ""
Write-Host "💾 Committing changes..." -ForegroundColor Yellow

# Configure git if needed (for CI environments)
$gitAuthorName = git config user.name 2>$null
if ([string]::IsNullOrEmpty($gitAuthorName)) {
    git config user.name "GitOps Bot"
    git config user.email "gitops-bot@springcrm.local"
}

# Add and commit the changes
git add $KustomizationFile
git commit -m $CommitMessage

# Get the new commit hash
$NewCommit = git rev-parse HEAD

Write-Host "✅ Successfully committed changes" -ForegroundColor Green
Write-Host "   New commit: $NewCommit" -ForegroundColor Gray
Write-Host "   File: $KustomizationFile" -ForegroundColor Gray

# Output for consumption by CI/CD
Write-Output "commit_sha=$NewCommit"
Write-Output "updated_services=$($UpdatedServices -join ' ')"
Write-Output "environment=$Environment"