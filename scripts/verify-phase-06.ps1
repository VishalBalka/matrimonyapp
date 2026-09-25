$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$path, [int]$line, [string]$rule) {
    $failures.Add("$path`:$line - $rule")
}

function Require-Text([string]$relativePath, [string]$pattern, [string]$rule) {
    $path = Join-Path $root $relativePath
    if (-not (Test-Path $path)) {
        Add-Failure $relativePath 0 "missing required file"
        return
    }
    $lines = Get-Content $path
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) { return }
    }
    Add-Failure $relativePath 0 $rule
}

Write-Host "PHASE 06 STATIC VERIFICATION"

$required = @(
    "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt",
    "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt",
    "android/app/src/main/java/com/matrimonyapp/data/remote/ApiErrorMapper.kt",
    "android/app/src/main/java/com/matrimonyapp/data/repository/AuthRepository.kt",
    "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt",
    "android/app/src/main/java/com/matrimonyapp/data/session/AuthState.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/auth/AuthenticatedScreen.kt",
    "android/app/src/debug/res/xml/network_security_config.xml",
    "android/app/src/debug/AndroidManifest.xml"
)
foreach ($file in $required) {
    if (-not (Test-Path (Join-Path $root $file))) { Add-Failure $file 0 "required Phase 06 file is missing" }
}

Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt" 'api/v1/auth/register' "Retrofit registration endpoint missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt" 'api/v1/auth/login' "Retrofit login endpoint missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt" 'api/v1/auth/logout' "Retrofit logout endpoint missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt" 'api/v1/auth/me' "Retrofit authenticated endpoint missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt" 'AndroidKeyStore' "Android Keystore-backed token storage missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt" 'AES/GCM/NoPadding' "AES-GCM token protection missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt" 'Authorization' "Authorization header handling missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt" 'response.code == 401' "401 token invalidation handling missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/ApiErrorMapper.kt" '429' "429 error mapping missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/remote/ApiErrorMapper.kt" '409' "409 error mapping missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/repository/AuthRepository.kt" 'tokenStore.clear' "repository session clearing missing"
Require-Text "android/app/src/main/java/com/matrimonyapp/data/session/AuthState.kt" 'Authenticated' "authenticated state missing"
Require-Text "android/app/src/main/AndroidManifest.xml" 'android.permission.INTERNET' "INTERNET permission missing"
Require-Text "android/app/src/debug/res/xml/network_security_config.xml" '10.0.2.2' "debug emulator host is missing"
Require-Text "android/app/src/debug/res/xml/network_security_config.xml" 'cleartextTrafficPermitted="false"' "debug network config does not fail closed by default"

$sourceRoot = Join-Path $root "android/app/src"
$sourceFiles = Get-ChildItem $sourceRoot -Recurse -File -Include *.kt,*.java,*.xml
foreach ($file in $sourceFiles) {
    $lines = Get-Content $file.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match 'r`n') { Add-Failure $file.FullName $($i + 1) "literal r`n found in source" }
        if ($line -match 'hostnameVerifier\s*\{\s*true\s*\}|TrustAll|trustAll|X509TrustManager\s*\{\s*\}') {
            Add-Failure $file.FullName $($i + 1) "dangerous TLS trust bypass detected"
        }
        if ($line -match 'Log\.[divew]\s*\(.*(password|token|Authorization)') {
            Add-Failure $file.FullName $($i + 1) "credential/token logging detected"
        }
        if ($line -match 'OTP|otp|OneTimePassword') {
            Add-Failure $file.FullName $($i + 1) "OTP found in active Android source"
        }
    }
}

$repoPath = Join-Path $root "android/app/src/main/java/com/matrimonyapp/data/repository/AuthRepository.kt"
if (Test-Path $repoPath) {
    $repoLines = Get-Content $repoPath
    $saveCount = ($repoLines | Select-String -Pattern 'tokenStore\.save\(').Count
    if ($saveCount -ne 1) { Add-Failure "android/app/src/main/java/com/matrimonyapp/data/repository/AuthRepository.kt" 0 "expected exactly one token save point" }
}

if ($failures.Count -gt 0) {
    Write-Host "PHASE 06 STATIC VERIFICATION: FAILED"
    $failures | ForEach-Object { Write-Host "FAIL: $_" }
    exit 1
}

Write-Host "PHASE 06 STATIC VERIFICATION: PASSED"
Write-Host "Checked Retrofit endpoints, repository/session state, Keystore token protection, 401 handling, error mapping, debug networking, and forbidden security patterns."
exit 0
