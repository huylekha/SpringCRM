# Manual Subdomain Setup for SpringCRM (Requires Administrator)
# This script adds all required subdomains to hosts file

$HostsFile = "$env:SystemRoot\System32\drivers\etc\hosts"
$IP = "127.0.0.1"
$Domain = "springcrm.com"

# All subdomains needed for SpringCRM
$Subdomains = @(
    "api",           # API Gateway
    "grafana",       # Grafana monitoring  
    "prometheus",    # Prometheus metrics
    "alertmanager",  # AlertManager
    "dev",           # Development environment
    "staging",       # Staging environment
    "prod"           # Production environment
)

Write-Host "🔧 Adding SpringCRM Subdomains to Hosts File" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host ""

# Check if running as Administrator
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "❌ This script requires Administrator privileges!" -ForegroundColor Red
    Write-Host ""
    Write-Host "🚀 Please run PowerShell as Administrator:" -ForegroundColor Cyan
    Write-Host "   1. Close this PowerShell window" -ForegroundColor Gray
    Write-Host "   2. Right-click PowerShell → 'Run as Administrator'" -ForegroundColor Gray
    Write-Host "   3. cd 'D:\Azure\Repo\SpringCRM'" -ForegroundColor Gray
    Write-Host "   4. .\scripts\add-subdomains-manual.ps1" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🔗 Or copy these entries manually to $HostsFile:" -ForegroundColor Yellow
    Write-Host ""
    foreach ($subdomain in $Subdomains) {
        Write-Host "   $IP`t$subdomain.$Domain" -ForegroundColor White
    }
    Write-Host ""
    exit 1
}

try {
    # Read current hosts file
    $CurrentContent = Get-Content $HostsFile
    
    # Create backup
    $BackupFile = "$HostsFile.backup.$(Get-Date -Format 'yyyyMMdd-HHmmss')"
    Copy-Item $HostsFile $BackupFile -Force
    Write-Host "✅ Created backup: $BackupFile" -ForegroundColor Green
    
    # Add each subdomain if not already exists
    $UpdatedContent = $CurrentContent
    $AddedCount = 0
    
    foreach ($subdomain in $Subdomains) {
        $FullDomain = "$subdomain.$Domain"
        $ExistingEntry = $UpdatedContent | Where-Object { $_ -match "^\s*\d+\.\d+\.\d+\.\d+\s+$FullDomain\s*$" }
        
        if (-not $ExistingEntry) {
            Write-Host "➕ Adding $FullDomain..." -ForegroundColor Green
            $UpdatedContent += "$IP`t$FullDomain"
            $AddedCount++
        } else {
            Write-Host "✅ $FullDomain already exists" -ForegroundColor Gray
        }
    }
    
    # Write updated content
    $UpdatedContent | Out-File -FilePath $HostsFile -Encoding ASCII -Force
    Write-Host ""
    Write-Host "✅ Successfully updated hosts file ($AddedCount new entries)" -ForegroundColor Green
    
    # Flush DNS cache
    ipconfig /flushdns | Out-Null
    Write-Host "🔄 DNS cache flushed" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "🎉 Subdomain setup completed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🔗 Available URLs:" -ForegroundColor Cyan
    Write-Host "   Main App: http://$Domain" -ForegroundColor Gray
    Write-Host "   API Gateway: http://api.$Domain" -ForegroundColor Gray
    Write-Host "   Grafana: http://grafana.$Domain" -ForegroundColor Gray
    Write-Host "   Prometheus: http://prometheus.$Domain" -ForegroundColor Gray
    Write-Host "   Development: http://dev.$Domain" -ForegroundColor Gray
    Write-Host ""
    Write-Host "🧪 Test Commands:" -ForegroundColor Cyan
    Write-Host "   curl http://api.$Domain/actuator/health" -ForegroundColor Gray
    Write-Host "   curl http://grafana.$Domain" -ForegroundColor Gray
    Write-Host "   curl http://dev.$Domain/auth-service/actuator/health" -ForegroundColor Gray
    
} catch {
    Write-Host "❌ Failed to update hosts file: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "💡 Make sure you're running as Administrator" -ForegroundColor Yellow
    exit 1
}