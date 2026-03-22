# Setup Local Domain for SpringCRM
# This script configures springcrm.com to point to localhost for local testing

param(
    [string]$Domain = "springcrm.com",
    [string]$IP = "127.0.0.1",
    [switch]$Remove,
    [switch]$Force
)

# Require Administrator privileges
if (-NOT ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Host "❌ This script requires Administrator privileges!" -ForegroundColor Red
    Write-Host "   Please run PowerShell as Administrator and try again." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "💡 Quick way: Right-click PowerShell → 'Run as Administrator'" -ForegroundColor Cyan
    exit 1
}

$HostsFile = "$env:SystemRoot\System32\drivers\etc\hosts"
$BackupFile = "$env:SystemRoot\System32\drivers\etc\hosts.backup.$(Get-Date -Format 'yyyyMMdd-HHmmss')"

Write-Host "🔧 SpringCRM Local Domain Setup" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green
Write-Host ""

# Create backup
try {
    Copy-Item $HostsFile $BackupFile -Force
    Write-Host "✅ Created backup: $BackupFile" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create backup: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Read current hosts file
$HostsContent = Get-Content $HostsFile

# Check if domain already exists
$ExistingEntry = $HostsContent | Where-Object { $_ -match "^\s*\d+\.\d+\.\d+\.\d+\s+$Domain\s*$" }

if ($Remove) {
    # Remove domain entry
    if ($ExistingEntry) {
        Write-Host "🗑️  Removing domain entry for $Domain..." -ForegroundColor Yellow
        
        $NewContent = $HostsContent | Where-Object { $_ -notmatch "^\s*\d+\.\d+\.\d+\.\d+\s+$Domain\s*$" }
        
        try {
            $NewContent | Out-File -FilePath $HostsFile -Encoding ASCII -Force
            Write-Host "✅ Successfully removed $Domain from hosts file" -ForegroundColor Green
        } catch {
            Write-Host "❌ Failed to update hosts file: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "ℹ️  Domain $Domain not found in hosts file" -ForegroundColor Yellow
    }
    exit 0
}

# Add or update domain entry
if ($ExistingEntry) {
    if (-not $Force) {
        Write-Host "⚠️  Domain $Domain already exists in hosts file:" -ForegroundColor Yellow
        Write-Host "   $ExistingEntry" -ForegroundColor Gray
        Write-Host ""
        $Response = Read-Host "Do you want to update it? (y/N)"
        if ($Response -ne 'y' -and $Response -ne 'Y') {
            Write-Host "❌ Operation cancelled" -ForegroundColor Red
            exit 1
        }
    }
    
    Write-Host "🔄 Updating existing domain entry..." -ForegroundColor Yellow
    
    # Replace existing entry
    $NewContent = $HostsContent | ForEach-Object {
        if ($_ -match "^\s*\d+\.\d+\.\d+\.\d+\s+$Domain\s*$") {
            "$IP`t$Domain"
        } else {
            $_
        }
    }
} else {
    Write-Host "➕ Adding new domain entry..." -ForegroundColor Yellow
    
    # Add new entry
    $NewContent = $HostsContent + @("", "# SpringCRM Local Development", "$IP`t$Domain")
}

# Write updated hosts file
try {
    $NewContent | Out-File -FilePath $HostsFile -Encoding ASCII -Force
    Write-Host "✅ Successfully updated hosts file" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to update hosts file: $($_.Exception.Message)" -ForegroundColor Red
    
    # Restore backup
    try {
        Copy-Item $BackupFile $HostsFile -Force
        Write-Host "🔄 Restored backup file" -ForegroundColor Yellow
    } catch {
        Write-Host "❌ Failed to restore backup: $($_.Exception.Message)" -ForegroundColor Red
    }
    exit 1
}

# Flush DNS cache
Write-Host "🔄 Flushing DNS cache..." -ForegroundColor Yellow
try {
    ipconfig /flushdns | Out-Null
    Write-Host "✅ DNS cache flushed" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Failed to flush DNS cache, but domain should still work" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "🎉 Domain setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Configuration Summary:" -ForegroundColor Cyan
Write-Host "   Domain: $Domain" -ForegroundColor Gray
Write-Host "   IP: $IP" -ForegroundColor Gray
Write-Host "   Hosts file: $HostsFile" -ForegroundColor Gray
Write-Host "   Backup: $BackupFile" -ForegroundColor Gray
Write-Host ""
Write-Host "🔗 Test URLs:" -ForegroundColor Cyan
Write-Host "   Development: http://$Domain/dev/frontend/" -ForegroundColor Gray
Write-Host "   Staging: http://$Domain/staging/frontend/" -ForegroundColor Gray
Write-Host "   Production: https://$Domain/prod/frontend/" -ForegroundColor Gray
Write-Host ""
Write-Host "🧪 Health Check URLs:" -ForegroundColor Cyan
Write-Host "   Auth Service: http://$Domain/dev/auth-service/actuator/health" -ForegroundColor Gray
Write-Host "   CRM Service: http://$Domain/dev/crm-service/actuator/health" -ForegroundColor Gray
Write-Host "   API Gateway: http://$Domain/dev/api-gateway/actuator/health" -ForegroundColor Gray
Write-Host ""
Write-Host "💡 To remove this configuration later, run:" -ForegroundColor Cyan
Write-Host "   .\scripts\setup-local-domain.ps1 -Remove" -ForegroundColor Gray
Write-Host ""