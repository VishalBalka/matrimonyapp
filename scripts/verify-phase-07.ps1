$ErrorActionPreference = "Stop"

Write-Host "PHASE 07 STATIC VERIFICATION"

$root = Split-Path -Parent $PSScriptRoot
$failures = @()

function Check-File($relativePath) {
    $path = Join-Path $root $relativePath
    if (-not (Test-Path $path)) {
        $script:failures += "$relativePath:1 - required file is missing"
    }
}

function Check-Pattern($relativePath, $pattern, $rule) {
    $path = Join-Path $root $relativePath
    if (-not (Test-Path $path)) { return }
    $lines = Get-Content $path
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            $script:failures += "${relativePath}:$($i + 1) - $rule"
        }
    }
}

$required = @(
    "backend/src/main/resources/db/migration/V3__profile_schema.sql",
    "backend/src/main/java/com/matrimony/profile/ProfileController.java",
    "backend/src/main/java/com/matrimony/profile/ProfileService.java",
    "backend/src/main/java/com/matrimony/profile/ProfileRepository.java",
    "backend/src/main/java/com/matrimony/profile/ProfileValidator.java",
    "android/app/src/main/java/com/matrimonyapp/data/remote/ProfileApi.kt",
    "android/app/src/main/java/com/matrimonyapp/data/profile/Profile.kt",
    "android/app/src/main/java/com/matrimonyapp/data/profile/ProfileRepository.kt",
    "android/app/src/main/java/com/matrimonyapp/data/profile/ProfileValidation.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt"
)

foreach ($file in $required) { Check-File $file }

$controller = Join-Path $root "backend/src/main/java/com/matrimony/profile/ProfileController.java"
if (Test-Path $controller) {
    $text = Get-Content $controller -Raw
    if ($text -notmatch '@RequestMapping\("/api/v1/profile"\)') {
        $failures += "backend/src/main/java/com/matrimony/profile/ProfileController.java:1 - profile base path is missing"
    }
    if ($text -notmatch 'AuthAuthenticationFilter\.PRINCIPAL_ATTRIBUTE') {
        $failures += "backend/src/main/java/com/matrimony/profile/ProfileController.java:1 - authenticated principal is not used"
    }
    if ($text -match 'userId\s*[\),=]') {
        $failures += "backend/src/main/java/com/matrimony/profile/ProfileController.java:1 - client-controlled userId must not control ownership"
    }
}

$api = Join-Path $root "android/app/src/main/java/com/matrimonyapp/data/remote/ProfileApi.kt"
if (Test-Path $api) {
    $text = Get-Content $api -Raw
    if ($text -notmatch '@GET\("api/v1/profile"\)') {
        $failures += "android/app/src/main/java/com/matrimonyapp/data/remote/ProfileApi.kt:1 - GET profile endpoint missing"
    }
    if ($text -notmatch '@PUT\("api/v1/profile"\)') {
        $failures += "android/app/src/main/java/com/matrimonyapp/data/remote/ProfileApi.kt:1 - PUT profile endpoint missing"
    }
}

$migration = Join-Path $root "backend/src/main/resources/db/migration/V3__profile_schema.sql"
if (Test-Path $migration) {
    $text = Get-Content $migration -Raw
    foreach ($requiredSql in @("PRIMARY KEY", "REFERENCES app_user", "UNIQUE (user_id)", "created_at", "updated_at", "CHECK (gender IN ('MALE', 'FEMALE', 'OTHER'))")) {
        if ($text -notmatch [regex]::Escape($requiredSql)) {
            $failures += "backend/src/main/resources/db/migration/V3__profile_schema.sql:1 - required database rule missing: $requiredSql"
        }
    }
}

$scanRoots = @(
    (Join-Path $root "android/app/src"),
    (Join-Path $root "backend/src")
)

Get-ChildItem -Path $scanRoots -Recurse -File -Include *.kt,*.java,*.xml,*.sql | ForEach-Object {
    $relative = $_.FullName.Substring($root.Length + 1).Replace("\","/")
    $lines = Get-Content $_.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i].Contains('r`n')) {
            $failures += "${relative}:$($i + 1) - literal r`n text found"
        }
        if ($lines[$i] -match 'trustAll|TrustAll|hostnameVerifier\s*\{|NoopHostnameVerifier|SSLContext\.getInstance\("SSL"\)') {
            $failures += "${relative}:$($i + 1) - dangerous TLS configuration found"
        }
        if ($lines[$i] -match '(password|token|authorization).*(Log|println|print\()') {
            $failures += "${relative}:$($i + 1) - credential or token logging pattern found"
        }
    }
}

if ($failures.Count -gt 0) {
    Write-Host "PHASE 07 STATIC VERIFICATION: FAILED"
    $failures | ForEach-Object { Write-Host $_ }
    exit 1
}

Write-Host "PHASE 07 STATIC VERIFICATION: PASSED"
Write-Host "Checked profile API protection, migration constraints, Android profile data flow, and forbidden security patterns."
exit 0
