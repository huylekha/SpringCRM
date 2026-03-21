# GitHub Secrets Helper Script
# This script shows the values you need to copy to GitHub Secrets

Write-Host "🔐 GitHub Secrets Configuration Helper" -ForegroundColor Green
Write-Host "Copy these values to GitHub Repository → Settings → Secrets and variables → Actions" -ForegroundColor Cyan
Write-Host ""

# Check if .env file exists
$envFile = ".env"
if (-not (Test-Path $envFile)) {
    Write-Error "❌ .env file not found. Please run 'docker compose up' first to generate it."
    exit 1
}

# Read .env file
Write-Host "📄 Reading values from .env file..." -ForegroundColor Yellow
$envContent = Get-Content $envFile

Write-Host ""
Write-Host "=" * 60 -ForegroundColor Gray
Write-Host "REQUIRED GITHUB SECRETS" -ForegroundColor Green
Write-Host "=" * 60 -ForegroundColor Gray

foreach ($line in $envContent) {
    if ($line -match "^(JWT_PRIVATE_KEY|JWT_PUBLIC_KEY|NEXTAUTH_SECRET)=(.+)$") {
        $key = $matches[1]
        $value = $matches[2]
        
        Write-Host ""
        Write-Host "Secret Name: " -NoNewline -ForegroundColor Cyan
        Write-Host $key -ForegroundColor White
        Write-Host "Secret Value: " -NoNewline -ForegroundColor Cyan
        Write-Host $value -ForegroundColor Yellow
        Write-Host "─" * 40 -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "=" * 60 -ForegroundColor Gray
Write-Host "OPTIONAL GITHUB SECRETS" -ForegroundColor Yellow
Write-Host "=" * 60 -ForegroundColor Gray

foreach ($line in $envContent) {
    if ($line -match "^(SENTRY_DSN|KAFKA_BOOTSTRAP_SERVERS)=(.+)$") {
        $key = $matches[1]
        $value = $matches[2]
        
        Write-Host ""
        Write-Host "Secret Name: " -NoNewline -ForegroundColor Cyan
        Write-Host $key -ForegroundColor White
        Write-Host "Secret Value: " -NoNewline -ForegroundColor Cyan
        Write-Host $value -ForegroundColor Yellow
        Write-Host "─" * 40 -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "📋 Setup Instructions:" -ForegroundColor Green
Write-Host "1. Go to: https://github.com/YOUR_USERNAME/SpringCRM/settings/secrets/actions" -ForegroundColor White
Write-Host "2. Click 'New repository secret'" -ForegroundColor White
Write-Host "3. Copy each Secret Name and Secret Value above" -ForegroundColor White
Write-Host "4. Click 'Add secret' for each one" -ForegroundColor White
Write-Host ""
Write-Host "✅ After adding secrets, commit any change to trigger the CI/CD pipeline" -ForegroundColor Green
Write-Host ""