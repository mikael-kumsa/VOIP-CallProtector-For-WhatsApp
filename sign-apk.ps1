# PowerShell script to sign an unsigned APK
# Usage: .\sign-apk.ps1 -ApkPath "app\release\app-release.apk" -KeystorePath "app\release.keystore" -Alias "whatsapp-call-protector"

param(
    [Parameter(Mandatory=$true)]
    [string]$ApkPath,
    
    [Parameter(Mandatory=$true)]
    [string]$KeystorePath,
    
    [Parameter(Mandatory=$false)]
    [string]$Alias = "whatsapp-call-protector"
)

Write-Host "=========================================="
Write-Host "APK Signing Script"
Write-Host "=========================================="
Write-Host ""

# Check if APK exists
if (-not (Test-Path $ApkPath)) {
    Write-Host "ERROR: APK file not found at: $ApkPath" -ForegroundColor Red
    exit 1
}

# Check if keystore exists
if (-not (Test-Path $KeystorePath)) {
    Write-Host "ERROR: Keystore file not found at: $KeystorePath" -ForegroundColor Red
    Write-Host "Please generate the keystore first using generate-keystore.bat" -ForegroundColor Yellow
    exit 1
}

# Prompt for passwords
$secureKeystorePassword = Read-Host "Enter keystore password" -AsSecureString
$keystorePassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKeystorePassword))

$secureKeyPassword = Read-Host "Enter key password (or press Enter to use same as keystore)" -AsSecureString
$keyPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKeyPassword))

if ([string]::IsNullOrEmpty($keyPassword)) {
    $keyPassword = $keystorePassword
}

# Create aligned APK path
$alignedApkPath = $ApkPath -replace "\.apk$", "-aligned.apk"
$signedApkPath = $ApkPath -replace "\.apk$", "-signed.apk"

Write-Host ""
Write-Host "Step 1: Aligning APK..." -ForegroundColor Cyan
$zipalignPath = "$env:ANDROID_HOME\build-tools\34.0.0\zipalign.exe"
if (-not (Test-Path $zipalignPath)) {
    # Try to find zipalign in common locations
    $possiblePaths = @(
        "$env:ANDROID_HOME\build-tools\*\zipalign.exe",
        "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\zipalign.exe",
        "$env:ProgramFiles\Android\Android Studio\jbr\bin\zipalign.exe"
    )
    
    $zipalignPath = $null
    foreach ($path in $possiblePaths) {
        $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $zipalignPath = $found.FullName
            break
        }
    }
    
    if (-not $zipalignPath) {
        Write-Host "ERROR: zipalign not found. Please set ANDROID_HOME or install Android SDK build-tools." -ForegroundColor Red
        exit 1
    }
}

& $zipalignPath -v -p 4 $ApkPath $alignedApkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to align APK" -ForegroundColor Red
    exit 1
}

Write-Host "Step 2: Signing APK..." -ForegroundColor Cyan
$apksignerPath = "$env:ANDROID_HOME\build-tools\34.0.0\apksigner.bat"
if (-not (Test-Path $apksignerPath)) {
    $possiblePaths = @(
        "$env:ANDROID_HOME\build-tools\*\apksigner.bat",
        "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\apksigner.bat"
    )
    
    $apksignerPath = $null
    foreach ($path in $possiblePaths) {
        $found = Get-ChildItem -Path $path -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $apksignerPath = $found.FullName
            break
        }
    }
    
    if (-not $apksignerPath) {
        Write-Host "ERROR: apksigner not found. Please set ANDROID_HOME or install Android SDK build-tools." -ForegroundColor Red
        exit 1
    }
}

& $apksignerPath sign --ks $KeystorePath --ks-key-alias $Alias --ks-pass "pass:$keystorePassword" --key-pass "pass:$keyPassword" --out $signedApkPath $alignedApkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to sign APK" -ForegroundColor Red
    exit 1
}

Write-Host "Step 3: Verifying signature..." -ForegroundColor Cyan
& $apksignerPath verify --verbose $signedApkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Signature verification failed" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=========================================="
Write-Host "SUCCESS: APK signed successfully!" -ForegroundColor Green
Write-Host "=========================================="
Write-Host ""
Write-Host "Signed APK: $signedApkPath" -ForegroundColor Green
Write-Host ""
Write-Host "You can now replace the original APK with the signed one." -ForegroundColor Yellow
Write-Host ""

