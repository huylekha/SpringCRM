# Generate Wildcard TLS Certificate for SpringCRM
# This script generates a self-signed wildcard certificate for *.springcrm.com

param(
    [string]$OutputDir = "k8s/overlays/prod",
    [string]$Domain = "springcrm.com",
    [int]$ValidDays = 365,
    [switch]$ApplyToCluster,
    [switch]$Force
)

Write-Host "🔐 SpringCRM Wildcard Certificate Generator" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Green
Write-Host ""

# Check if OpenSSL is available
try {
    $null = openssl version
    Write-Host "✅ OpenSSL found" -ForegroundColor Green
} catch {
    Write-Host "❌ OpenSSL not found. Please install OpenSSL first." -ForegroundColor Red
    Write-Host "   Download from: https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor Yellow
    exit 1
}

# Create output directory if it doesn't exist
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
    Write-Host "📁 Created output directory: $OutputDir" -ForegroundColor Green
}

$KeyFile = Join-Path $OutputDir "wildcard-$Domain.key"
$CsrFile = Join-Path $OutputDir "wildcard-$Domain.csr"
$CrtFile = Join-Path $OutputDir "wildcard-$Domain.crt"
$ConfigFile = Join-Path $OutputDir "wildcard-cert.conf"

# Check if files already exist
if ((Test-Path $KeyFile) -or (Test-Path $CrtFile)) {
    if (-not $Force) {
        Write-Host "⚠️  Certificate files already exist:" -ForegroundColor Yellow
        Write-Host "   Key: $KeyFile" -ForegroundColor Gray
        Write-Host "   Cert: $CrtFile" -ForegroundColor Gray
        Write-Host ""
        $Response = Read-Host "Overwrite existing files? (y/N)"
        if ($Response -ne 'y' -and $Response -ne 'Y') {
            Write-Host "❌ Operation cancelled" -ForegroundColor Red
            exit 1
        }
    }
    
    Write-Host "🗑️  Removing existing certificate files..." -ForegroundColor Yellow
    Remove-Item $KeyFile -ErrorAction SilentlyContinue
    Remove-Item $CsrFile -ErrorAction SilentlyContinue  
    Remove-Item $CrtFile -ErrorAction SilentlyContinue
}

Write-Host "🔑 Generating private key..." -ForegroundColor Yellow
try {
    openssl genrsa -out $KeyFile 2048 2>$null
    Write-Host "✅ Private key generated: $KeyFile" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to generate private key: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "📝 Creating certificate signing request..." -ForegroundColor Yellow
try {
    openssl req -new -key $KeyFile -out $CsrFile -config $ConfigFile 2>$null
    Write-Host "✅ CSR created: $CsrFile" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to create CSR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "🎫 Generating self-signed certificate..." -ForegroundColor Yellow
try {
    openssl x509 -req -in $CsrFile -signkey $KeyFile -out $CrtFile -days $ValidDays -extensions v3_req -extfile $ConfigFile 2>$null
    Write-Host "✅ Certificate generated: $CrtFile" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed to generate certificate: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Clean up CSR file
Remove-Item $CsrFile -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "📋 Certificate Information:" -ForegroundColor Cyan
try {
    $CertInfo = openssl x509 -in $CrtFile -text -noout | Select-String "Subject:|DNS:|IP Address:|Not After"
    $CertInfo | ForEach-Object { Write-Host "   $($_.Line.Trim())" -ForegroundColor Gray }
} catch {
    Write-Host "   Certificate generated successfully" -ForegroundColor Gray
}

if ($ApplyToCluster) {
    Write-Host ""
    Write-Host "🚀 Applying certificate to Kubernetes cluster..." -ForegroundColor Yellow
    
    try {
        # Check if namespace exists
        $null = kubectl get namespace prod 2>$null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "📦 Creating prod namespace..." -ForegroundColor Yellow
            kubectl create namespace prod
        }
        
        # Delete existing secret if it exists
        kubectl delete secret wildcard-springcrm-tls-secret -n prod 2>$null
        
        # Create new secret
        kubectl create secret tls wildcard-springcrm-tls-secret --cert=$CrtFile --key=$KeyFile -n prod
        Write-Host "✅ Certificate applied to cluster as secret 'wildcard-springcrm-tls-secret'" -ForegroundColor Green
        
    } catch {
        Write-Host "❌ Failed to apply certificate to cluster: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "💡 You can manually apply it later using:" -ForegroundColor Cyan
        Write-Host "   kubectl create secret tls wildcard-springcrm-tls-secret --cert=$CrtFile --key=$KeyFile -n prod" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "🎉 Wildcard certificate generation complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📁 Generated Files:" -ForegroundColor Cyan
Write-Host "   Private Key: $KeyFile" -ForegroundColor Gray
Write-Host "   Certificate: $CrtFile" -ForegroundColor Gray
Write-Host "   Config: $ConfigFile" -ForegroundColor Gray
Write-Host ""
Write-Host "🔧 Manual Kubernetes Application:" -ForegroundColor Cyan
Write-Host "   kubectl create secret tls wildcard-springcrm-tls-secret --cert=$CrtFile --key=$KeyFile -n prod" -ForegroundColor Gray
Write-Host ""
Write-Host "🌐 Supported Domains:" -ForegroundColor Cyan
Write-Host "   - $Domain" -ForegroundColor Gray
Write-Host "   - *.$Domain" -ForegroundColor Gray
Write-Host "   - api.$Domain" -ForegroundColor Gray
Write-Host "   - grafana.$Domain" -ForegroundColor Gray
Write-Host "   - prometheus.$Domain" -ForegroundColor Gray
Write-Host "   - dev.$Domain, staging.$Domain, prod.$Domain" -ForegroundColor Gray
Write-Host ""