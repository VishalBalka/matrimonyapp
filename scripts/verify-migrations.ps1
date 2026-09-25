$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$failures = 0

function Pass($message) {
    Write-Host "PASS: $message"
}

function Fail($path, $line, $rule, $message) {
    $script:failures++
    if ($line) {
        Write-Host "FAIL: ${path}:$line - $rule - $message"
    } else {
        Write-Host "FAIL: $path - $rule - $message"
    }
}

Write-Host "PHASE 12 MIGRATION VERIFICATION"

$migrationDir = Join-Path $root "backend/src/main/resources/db/migration"

$expected = @(
    "V1__initial_auth_schema.sql",
    "V2__auth_timestamps.sql",
    "V3__profile_schema.sql",
    "V4__profile_enrichment.sql",
    "V5__security_privacy_admin.sql"
)

foreach ($name in $expected) {
    $path = Join-Path $migrationDir $name
    if (Test-Path $path -PathType Leaf) {
        Pass "Required migration - $name"
    } else {
        Fail "backend/src/main/resources/db/migration/$name" $null `
            "migration-file" "Required migration file is missing."
    }
}

$files = @(Get-ChildItem $migrationDir -File -Filter "V*.sql")

$versions = @()

foreach ($file in $files) {
    if ($file.Name -match '^V([0-9]+)__.+\.sql$') {
        $versions += [int]$Matches[1]
    } else {
        Fail "backend/src/main/resources/db/migration/$($file.Name)" $null `
            "migration-naming" "Migration does not use the required V<number>__description.sql format."
    }
}

$duplicateVersions = @(
    $versions | Group-Object | Where-Object { $_.Count -gt 1 }
)

if ($duplicateVersions.Count -eq 0) {
    Pass "No duplicate migration versions"
} else {
    foreach ($group in $duplicateVersions) {
        Fail "backend/src/main/resources/db/migration" $null `
            "duplicate-version" "Migration version V$($group.Name) appears more than once."
    }
}

$actualVersions = @($versions | Sort-Object)
$expectedVersions = @(1, 2, 3, 4, 5)

if (($actualVersions -join ",") -eq ($expectedVersions -join ",")) {
    Pass "Migration sequence is exactly V1 through V5"
} else {
    Fail "backend/src/main/resources/db/migration" $null `
        "migration-sequence" "Expected exactly V1,V2,V3,V4,V5 but found $($actualVersions -join ',')."
}

$flywayConfig = Join-Path $root "backend/src/main/resources/application.yml"
if ((Test-Path $flywayConfig) -and (Get-Content $flywayConfig -Raw) -match '(?m)^\s*flyway:\s*$') {
    Pass "Flyway configuration is present"
} else {
    Fail "backend/src/main/resources/application.yml" $null `
        "flyway-config" "Flyway configuration block was not found."
}

$v4 = Join-Path $migrationDir "V4__profile_enrichment.sql"
if (Test-Path $v4) {
    $content = Get-Content $v4 -Raw

    if ($content -match '(?i)country') {
        Pass "V4 contains country profile field"
    } else {
        Fail "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" $null `
            "profile-country" "Country field was not found."
    }

    if ($content -match '(?i)state') {
        Pass "V4 contains state profile field"
    } else {
        Fail "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" $null `
            "profile-state" "State field was not found."
    }

    if ($content -match '(?i)phone') {
        Pass "V4 contains private phone field"
    } else {
        Fail "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" $null `
            "profile-phone" "Phone field was not found."
    }

    if ($content -match '(?i)photo_storage_key') {
        Pass "V4 contains photo storage key"
    } else {
        Fail "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" $null `
            "profile-photo" "Photo storage key was not found."
    }

    if ($content -match '(?i)unique\s*\(\s*photo_storage_key\s*\)|photo_storage_key[^;\n]*(unique|UNIQUE)') {
        Pass "Photo storage key uniqueness constraint is present"
    } else {
        Fail "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" $null `
            "photo-uniqueness" "Photo storage key uniqueness constraint was not found."
    }

    if ($content -match '(?i)create\s+index[^;\n]*country[^;\n]*state[^;\n]*city|create\s+index[^;\n]*\(.*country.*state.*city') {
        Pass "Country/state/city location index is present"
    } else {
        Fail "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" $null `
            "location-index" "Country/state/city location index was not found."
    }
}

if ($failures -eq 0) {
    Write-Host "PHASE 12 MIGRATION VERIFICATION: PASS"
    exit 0
}

Write-Host "PHASE 12 MIGRATION VERIFICATION: FAIL ($failures failure(s))"
exit 1
$v5 = Join-Path $migrationDir "V5__security_privacy_admin.sql"
if (Test-Path $v5) {
    $content = Get-Content $v5 -Raw
    foreach ($required in @("mfa_factor","mfa_challenge","user_block","user_report","app_notification","admin_audit_log","verification_status","background_check_status","linkedin_url","salary_range","profile_visibility")) {
        if ($content -match "(?i)$required") { Pass "V5 contains $required" }
        else { Fail "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql" $null "v5-field" "V5 is missing $required." }
    }
} else {
    Fail "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql" $null "v5-file" "V5 migration is missing."
}


