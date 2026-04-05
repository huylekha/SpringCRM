# Setup Local Domain for SpringCRM - Microservices Architecture
param(
    [string]$Domain = "springcrm.com",
    [string]$IP = "127.0.0.1",
    [switch]$Remove,
    [switch]$Force,
    [switch]$BatchSetup,
    [string[]]$Subdomains = @()
)

# Check if running as Administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "❌ This script requires Administrator privileges!" -ForegroundColor Red
    Write-Host "   Please run PowerShell as Administrator and try again." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "🚀 Quick way: Right-click PowerShell → 'Run as Administrator'" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

# Configuration
$HostsFile = "$env:SystemRoot\System32\drivers\etc\hosts"
$BackupFile = "$env:SystemRoot\System32\drivers\etc\hosts.backup.$(Get-Date -Format 'yyyyMMdd-HHmmss')"

# Default subdomains for SpringCRM - Microservices Architecture
$DefaultSubdomains = @(
    "dev.api",       # dev.api.springcrm.com - Development API Gateway
    "staging.api",   # staging.api.springcrm.com - Staging API Gateway  
    "api",           # api.springcrm.com - Production API Gateway
    "dev",           # dev.springcrm.com - Development Frontend
    "staging",       # staging.springcrm.com - Staging Frontend
    "grafana",       # grafana.springcrm.com - Monitoring (shared)
    "prometheus",    # prometheus.springcrm.com - Metrics (shared)
    "alertmanager"   # alertmanager.springcrm.com - Alerts (shared)
)

# Determine subdomains to use
$SubdomainsToUse = if ($Subdomains.Count -gt 0) { $Subdomains } else { $DefaultSubdomains }

# Determine domains to process
$DomainsToProcess = @()
if ($BatchSetup) {
    # Add main domain
    $DomainsToProcess += $Domain
    
    # Add subdomains
    foreach ($subdomain in $SubdomainsToUse) {
        $DomainsToProcess += "$subdomain.$Domain"
    }
} else {
    $DomainsToProcess += $Domain
}

Write-Host "🔧 SpringCRM Microservices Domain Setup" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

if ($BatchSetup) {
    Write-Host "📦 Batch mode: Setting up microservices subdomain architecture" -ForegroundColor Cyan
    Write-Host "   Main domain: $Domain" -ForegroundColor Gray
    Write-Host "   Subdomains: $($SubdomainsToUse -join ', ')" -ForegroundColor Gray
    Write-Host ""
}

# Create backup
try {
    Copy-Item $HostsFile $BackupFile -Force
    Write-Host "✅ Created backup: $BackupFile" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create backup: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Read current hosts file
try {
    $CurrentContent = Get-Content $HostsFile -ErrorAction Stop
} catch {
    Write-Host "❌ Failed to read hosts file: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Process all domains
$UpdatedContent = $CurrentContent

foreach ($DomainToProcess in $DomainsToProcess) {
    $ExistingEntry = $UpdatedContent | Where-Object { $_ -match "^\s*\d+\.\d+\.\d+\.\d+\s+$DomainToProcess\s*$" }
    
    if ($Remove) {
        if ($ExistingEntry) {
            Write-Host "🗑️  Removing $DomainToProcess..." -ForegroundColor Yellow
            $UpdatedContent = $UpdatedContent | Where-Object { $_ -notmatch "^\s*\d+\.\d+\.\d+\.\d+\s+$DomainToProcess\s*$" }
        } else {
            Write-Host "ℹ️  $DomainToProcess not found in hosts file" -ForegroundColor Gray
        }
    } else {
        if ($ExistingEntry) {
            if (-not $Force) {
                Write-Host "⚠️  $DomainToProcess already exists: $ExistingEntry" -ForegroundColor Yellow
                $Response = Read-Host "Update $DomainToProcess? (y/N)"
                if ($Response -notmatch '^[Yy]') {
                    Write-Host "⏭️  Skipping $DomainToProcess" -ForegroundColor Gray
                    continue
                }
            }
            Write-Host "🔄 Updating $DomainToProcess..." -ForegroundColor Cyan
            $UpdatedContent = $UpdatedContent | Where-Object { $_ -notmatch "^\s*\d+\.\d+\.\d+\.\d+\s+$DomainToProcess\s*$" }
            $UpdatedContent += "$IP`t$DomainToProcess"
        } else {
            Write-Host "➕ Adding $DomainToProcess..." -ForegroundColor Green
            $UpdatedContent += "$IP`t$DomainToProcess"
        }
    }
}

# Write updated content back to hosts file
try {
    $UpdatedContent | Out-File -FilePath $HostsFile -Encoding ASCII -Force
    Write-Host "✅ Successfully updated hosts file" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to update hosts file: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Make sure you're running as Administrator" -ForegroundColor Yellow
    exit 1
}

# Flush DNS cache
try {
    ipconfig /flushdns | Out-Null
    Write-Host "🔄 DNS cache flushed" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Failed to flush DNS cache, but domain should still work" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "🎉 Microservices domain setup completed!" -ForegroundColor Green
Write-Host ""

if (-not $Remove) {
    Write-Host "🌐 New Microservices Architecture URLs:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "📱 Frontend Applications:" -ForegroundColor Yellow
    Write-Host "   Development: http://dev.$Domain" -ForegroundColor Gray
    Write-Host "   Staging: http://staging.$Domain" -ForegroundColor Gray
    Write-Host "   Production: http://$Domain" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🔌 API Gateways:" -ForegroundColor Yellow
    Write-Host "   Development: http://dev.api.$Domain" -ForegroundColor Gray
    Write-Host "   Staging: http://staging.api.$Domain" -ForegroundColor Gray
    Write-Host "   Production: http://api.$Domain" -ForegroundColor Gray
    Write-Host ""
    Write-Host "📊 Monitoring (Shared):" -ForegroundColor Yellow
    Write-Host "   Grafana: http://grafana.$Domain" -ForegroundColor Gray
    Write-Host "   Prometheus: http://prometheus.$Domain" -ForegroundColor Gray
    Write-Host "   AlertManager: http://alertmanager.$Domain" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🧪 Example API Calls:" -ForegroundColor Cyan
    Write-Host "   Auth Service: http://dev.api.$Domain/api/v1/auth/actuator/health" -ForegroundColor Gray
    Write-Host "   CRM Service: http://dev.api.$Domain/api/v1/customers" -ForegroundColor Gray
    Write-Host "   Gateway Health: http://dev.api.$Domain/actuator/health" -ForegroundColor Gray
    Write-Host ""
    Write-Host "💡 Usage Examples:" -ForegroundColor Cyan
    Write-Host "   Setup all subdomains: .\scripts\setup-local-domain.ps1 -BatchSetup" -ForegroundColor Gray
    Write-Host "   Setup custom subdomains: .\scripts\setup-local-domain.ps1 -BatchSetup -Subdomains @('dev.api','staging.api')" -ForegroundColor Gray
    Write-Host "   Remove all domains: .\scripts\setup-local-domain.ps1 -Remove -BatchSetup" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🎯 Next Steps:" -ForegroundColor Cyan
    Write-Host "   1. Apply new Kubernetes ingress configurations" -ForegroundColor Gray
    Write-Host "   2. Update API Gateway routing" -ForegroundColor Gray
    Write-Host "   3. Test microservices communication" -ForegroundColor Gray
} else {
    Write-Host "✅ All specified domains have been removed from hosts file" -ForegroundColor Green
    Write-Host "💡 Backup created at: $BackupFile" -ForegroundColor Cyan
}

Write-Host ""
Write-Host "📋 Backup location: $BackupFile" -ForegroundColor Cyan
Write-Host "🔧 Hosts file location: $HostsFile" -ForegroundColor Cyan
Write-Host ""