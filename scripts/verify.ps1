$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$failures = 0

function Pass($message) {
    Write-Host "PASS: $message"
}

function Fail($path, $line, $rule, $message) {
    $script:failures++
    if ($line) {
        Write-Host "FAIL: $path`:$line - $rule - $message"
    } else {
        Write-Host "FAIL: $path - $rule - $message"
    }
}

function Require-File($relativePath) {
    $path = Join-Path $root $relativePath
    if (Test-Path $path -PathType Leaf) {
        Pass "Required file - $relativePath"
    } else {
        Fail $relativePath $null "required-file" "File does not exist."
    }
}

function Check-PatternAbsent($relativePath, $pattern, $rule, $description) {
    $path = Join-Path $root $relativePath
    if (-not (Test-Path $path -PathType Leaf)) {
        return
    }

    $lines = Get-Content $path
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            Fail $relativePath ($i + 1) $rule $description
        }
    }
}

Write-Host "PHASE 12 STATIC VERIFICATION"

$requiredFiles = @(
    "CLAUDE.md",
    "docker-compose.yml",
    "backend/src/main/resources/application.yml",
    "backend/src/main/resources/db/migration/V1__initial_auth_schema.sql",
    "backend/src/main/resources/db/migration/V2__auth_timestamps.sql",
    "backend/src/main/resources/db/migration/V3__profile_schema.sql",
    "backend/src/main/resources/db/migration/V4__profile_enrichment.sql",
    "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql",
    "backend/src/main/java/com/matrimony/auth/AuthController.java",
    "backend/src/main/java/com/matrimony/auth/AuthAuthenticationFilter.java",
    "backend/src/main/java/com/matrimony/auth/SessionService.java",
    "backend/src/main/java/com/matrimony/auth/SessionRepository.java",
    "backend/src/main/java/com/matrimony/auth/BCryptPasswordHasher.java",
    "backend/src/main/java/com/matrimony/auth/MfaService.java",
    "backend/src/main/java/com/matrimony/auth/MfaCrypto.java",
    "backend/src/main/java/com/matrimony/auth/Totp.java",
    "backend/src/main/java/com/matrimony/security/SecurityController.java",
    "backend/src/main/java/com/matrimony/notification/NotificationController.java",
    "backend/src/main/java/com/matrimony/admin/AdminController.java",
    "backend/src/main/java/com/matrimony/profile/ProfileController.java",
    "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt",
    "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt",
    "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt",
    "android/app/src/main/java/com/matrimonyapp/data/repository/AuthRepository.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupMotion.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/welcome/WelcomeScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/auth/AuthenticatedScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt"
)

foreach ($file in $requiredFiles) {
    Require-File $file
}

# Forbidden active authentication mechanisms.
$authFiles = @(
    "backend/src/main/java/com/matrimony/auth",
    "android/app/src/main/java/com/matrimonyapp"
)

foreach ($dir in $authFiles) {
    $path = Join-Path $root $dir
    if (Test-Path $path) {
        Get-ChildItem $path -Recurse -File -Include *.java,*.kt | ForEach-Object {
            $relative = $_.FullName.Substring($root.Length + 1)
            Check-PatternAbsent $relative '(?i)\bOTP\b|one[- ]time password|SMS authentication|social login|password reset' `
                "authentication-scope" `
                "Inactive authentication mechanism appears in active source."
        }
    }
}

# Dangerous security patterns.
foreach ($dir in @(
    "backend/src/main/java",
    "android/app/src/main/java"
)) {
    $path = Join-Path $root $dir
    if (Test-Path $path) {
        Get-ChildItem $path -Recurse -File -Include *.java,*.kt | ForEach-Object {
            $relative = $_.FullName.Substring($root.Length + 1)
            Check-PatternAbsent $relative 'TrustAll|trustAll|HostnameVerifier\s*\{|ALLOW_ALL|sslSocketFactory\s*\(\s*.*unsafe|X509TrustManager' `
                "tls-security" `
                "Potential TLS verification bypass or trust-all configuration."
        }
    }
}

# No arbitrary cleartext Android networking.
$apiClient = Join-Path $root "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt"
if (Test-Path $apiClient) {
    $content = Get-Content $apiClient -Raw
    if ($content -match 'http://') {
        if ($content -notmatch '192\.168\.29\.82|10\.0\.2\.2') {
            Fail "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt" $null `
                "network-security" "Cleartext HTTP host is not an approved local debug host."
        } else {
            Pass "Android debug HTTP host is restricted to approved local development hosts"
        }
    } else {
        Pass "Android API client contains no cleartext HTTP URL"
    }
}

# Session TTL must remain 15 minutes.
$appYml = Join-Path $root "backend/src/main/resources/application.yml"
$appLines = Get-Content $appYml
$ttlLine = $appLines | Select-String '^\s*ttl:\s*15m\s*$'
if ($ttlLine) {
    Pass "Session TTL is configured to 15 minutes"
} else {
    Fail "backend/src/main/resources/application.yml" $null `
        "session-ttl" "Expected auth.session.ttl to be 15m."
}

# Required Flyway sequence.
$migrationDir = Join-Path $root "backend/src/main/resources/db/migration"
$expectedMigrations = @(
    "V1__initial_auth_schema.sql",
    "V2__auth_timestamps.sql",
    "V3__profile_schema.sql",
    "V4__profile_enrichment.sql",
    "V5__security_privacy_admin.sql"
)
$actualMigrations = @(Get-ChildItem $migrationDir -File -Filter "V*.sql" | Sort-Object Name | Select-Object -ExpandProperty Name)
if (@(Compare-Object $expectedMigrations $actualMigrations).Count -eq 0) {
    Pass "Flyway migrations V1 through V5 are present with no extra V-numbered migration"
} else {
    Fail "backend/src/main/resources/db/migration" $null `
        "migration-sequence" "Expected exactly V1 through V5."
}

# No literal PowerShell newline escape accidentally embedded in source.
Get-ChildItem (Join-Path $root "android/app/src/main") -Recurse -File -Include *.kt | ForEach-Object {
    $relative = $_.FullName.Substring($root.Length + 1)
    Check-PatternAbsent $relative 'r`n' "source-integrity" "Literal r`n text found in Kotlin source."
}

# No obvious credential/token logging.
Get-ChildItem (Join-Path $root "backend/src/main/java") -Recurse -File -Include *.java | ForEach-Object {
    $relative = $_.FullName.Substring($root.Length + 1)
    Check-PatternAbsent $relative '(?i)(log|logger)\.\w+\([^)]*(password|token|authorization)' `
        "secret-logging" "Potential password, token, or Authorization logging."
}

# Docker must require a password from the environment.
$compose = Join-Path $root "docker-compose.yml"
$composeContent = Get-Content $compose -Raw
if ($composeContent -match 'POSTGRES_PASSWORD:\s*\$\{POSTGRES_PASSWORD:\?') {
    Pass "Docker PostgreSQL password is required from environment"
} else {
    Fail "docker-compose.yml" $null "credential-management" "PostgreSQL password is not environment-required."
}

if ($failures -eq 0) {
    Write-Host "PHASE 12 STATIC VERIFICATION: PASS"
    exit 0
}

Write-Host "PHASE 12 STATIC VERIFICATION: FAIL ($failures failure(s))"
exit 1
