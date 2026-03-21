# GitHub Actions CI/CD Quick Setup Script
# Run this script as Administrator on Windows

param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubUrl,
    
    [Parameter(Mandatory=$true)]
    [string]$RunnerToken,
    
    [string]$RunnerName = "local-runner"
)

Write-Host "🚀 Setting up GitHub Actions Self-Hosted Runner..." -ForegroundColor Green

# Check if running as Administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Error "This script must be run as Administrator. Please restart PowerShell as Administrator."
    exit 1
}

# Check Docker
Write-Host "📋 Checking prerequisites..." -ForegroundColor Yellow
try {
    docker --version | Out-Null
    docker compose version | Out-Null
    Write-Host "✅ Docker and Docker Compose are installed" -ForegroundColor Green
} catch {
    Write-Error "❌ Docker or Docker Compose not found. Please install Docker Desktop first."
    exit 1
}

# Create runner directory
$runnerDir = "C:\actions-runner"
if (Test-Path $runnerDir) {
    Write-Host "📁 Removing existing runner directory..." -ForegroundColor Yellow
    Remove-Item $runnerDir -Recurse -Force
}

Write-Host "📁 Creating runner directory: $runnerDir" -ForegroundColor Yellow
New-Item -ItemType Directory -Path $runnerDir | Out-Null
Set-Location $runnerDir

# Download runner
Write-Host "⬇️ Downloading GitHub Actions runner..." -ForegroundColor Yellow
$runnerUrl = "https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-win-x64-2.311.0.zip"
$runnerZip = "actions-runner-win-x64-2.311.0.zip"

try {
    Invoke-WebRequest -Uri $runnerUrl -OutFile $runnerZip
    Write-Host "✅ Runner downloaded successfully" -ForegroundColor Green
} catch {
    Write-Error "❌ Failed to download runner: $_"
    exit 1
}

# Extract runner
Write-Host "📦 Extracting runner..." -ForegroundColor Yellow
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory("$PWD\$runnerZip", "$PWD")
    Remove-Item $runnerZip
    Write-Host "✅ Runner extracted successfully" -ForegroundColor Green
} catch {
    Write-Error "❌ Failed to extract runner: $_"
    exit 1
}

# Configure runner
Write-Host "⚙️ Configuring runner..." -ForegroundColor Yellow
try {
    & .\config.cmd --url $GitHubUrl --token $RunnerToken --name $RunnerName --work "_work" --replace
    Write-Host "✅ Runner configured successfully" -ForegroundColor Green
} catch {
    Write-Error "❌ Failed to configure runner: $_"
    exit 1
}

# Install and start service
Write-Host "🔧 Installing runner as Windows service..." -ForegroundColor Yellow
try {
    & .\svc.sh install
    & .\svc.sh start
    Write-Host "✅ Runner service installed and started" -ForegroundColor Green
} catch {
    Write-Error "❌ Failed to install/start runner service: $_"
    exit 1
}

# Verify service
Write-Host "🔍 Verifying runner status..." -ForegroundColor Yellow
$service = Get-Service -Name "actions.runner.*" -ErrorAction SilentlyContinue
if ($service -and $service.Status -eq "Running") {
    Write-Host "✅ Runner service is running" -ForegroundColor Green
} else {
    Write-Warning "⚠️ Runner service may not be running properly. Check manually."
}

Write-Host ""
Write-Host "🎉 GitHub Actions Runner Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Go to your GitHub repository settings to verify the runner is online"
Write-Host "2. Configure GitHub Secrets (see SETUP-GITHUB-ACTIONS.md)"
Write-Host "3. Make a commit to trigger the CI/CD pipeline"
Write-Host ""
Write-Host "Runner location: $runnerDir" -ForegroundColor Gray
Write-Host "Service management: Get-Service 'actions.runner.*'" -ForegroundColor Gray
Write-Host ""