$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$errors = @()

function Fail-Rule([string]$path, [int]$line, [string]$rule) {
    $script:errors += "ERROR: ${path}:$line - $rule"
}

$required = @(
    "docker-compose.yml",
    "backend/build.gradle.kts",
    "backend/settings.gradle.kts",
    "backend/src/main/java/com/matrimony/Application.java",
    "backend/src/main/java/com/matrimony/core/HealthController.java",
    "backend/src/main/resources/application.yml",
    "backend/src/main/resources/db/migration/V1__initial_auth_schema.sql",
    "backend/src/test/java/com/matrimony/ApplicationContextTest.java"
)

foreach ($relative in $required) {
    $full = Join-Path $root $relative
    if (-not (Test-Path -LiteralPath $full)) {
        Fail-Rule $relative 1 "required Phase 04 file is missing"
    }
}

$compose = Join-Path $root "docker-compose.yml"
if (Test-Path $compose) {
    $lineNo = 0
    Get-Content -LiteralPath $compose | ForEach-Object {
        $lineNo++
        if ($_ -match "image:\s+[^/\s]+:latest\b") {
            Fail-Rule "docker-compose.yml" $lineNo "floating Docker image tag is forbidden"
        }
    }
}

$migrationDir = Join-Path $root "backend/src/main/resources/db/migration"
if (Test-Path $migrationDir) {
    $versions = @{}
    Get-ChildItem -LiteralPath $migrationDir -File -Filter "V*.sql" | ForEach-Object {
        if ($_.Name -match '^V([^_]+)__') {
            $version = $Matches[1]
            if ($versions.ContainsKey($version)) {
                Fail-Rule ("backend/src/main/resources/db/migration/" + $_.Name) 1 "duplicate Flyway migration version"
            }
            $versions[$version] = $_.Name
        }
    }
}

$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $root "backend") -Recurse -File -Include *.java,*.kt,*.kts,*.yml,*.yaml,*.sql -ErrorAction SilentlyContinue
foreach ($file in $sourceFiles) {
    $lineNo = 0
    Get-Content -LiteralPath $file.FullName | ForEach-Object {
        $lineNo++
        $code = $_ -replace '//.*$', ''
        if ($_ -match 'r`n') {
            Fail-Rule $file.FullName $lineNo "literal r`n sequence is forbidden"
        }
        if ($code -match '(?i)(password|token|authorization).*(log|println|print\()') {
            Fail-Rule $file.FullName $lineNo "secret logging pattern is forbidden"
        }
        if ($code -match '(?i)trustAll|TrustAll|hostnameVerifier\s*\{\s*true\s*\}|NoopHostnameVerifier') {
            Fail-Rule $file.FullName $lineNo "dangerous TLS bypass is forbidden"
        }
    }
}

if ($errors.Count -gt 0) {
    Write-Host "PHASE 04 STATIC VERIFICATION: FAILED"
    $errors | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host "PHASE 04 STATIC VERIFICATION: PASSED"
Write-Host "Checked backend foundation files, Docker image pinning, Flyway uniqueness, secret/TLS safeguards, and required schema artifacts."
exit 0
