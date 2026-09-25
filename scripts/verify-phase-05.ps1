$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$errors = [System.Collections.Generic.List[string]]::new()

function Add-RuleError([string]$Path, [int]$Line, [string]$Rule) {
    $errors.Add("ERROR: ${Path}:$Line - $Rule")
}

function Get-Text([string]$RelativePath) {
    $path = Join-Path $repoRoot $RelativePath
    if (-not (Test-Path $path)) {
        Add-RuleError $RelativePath 1 "required file is missing"
        return ''
    }
    return Get-Content -Raw $path
}

Write-Host "PHASE 05 STATIC VERIFICATION"

$required = @(
    'backend/src/main/java/com/matrimony/auth/AuthController.java',
    'backend/src/main/java/com/matrimony/auth/AuthService.java',
    'backend/src/main/java/com/matrimony/auth/SessionService.java',
    'backend/src/main/java/com/matrimony/auth/SessionRepository.java',
    'backend/src/main/java/com/matrimony/auth/PasswordHasher.java',
    'backend/src/main/java/com/matrimony/auth/BCryptPasswordHasher.java',
    'backend/src/main/java/com/matrimony/auth/InMemoryRateLimiter.java',
    'backend/src/main/resources/application-local.yml',
    'backend/src/main/resources/db/migration/V2__auth_timestamps.sql',
    'docs/handoff/phase-05.md'
)

foreach ($file in $required) {
    [void](Get-Text $file)
}

$controller = Get-Text 'backend/src/main/java/com/matrimony/auth/AuthController.java'

foreach ($route in @('/register','/login','/logout','/me')) {
    if ($controller -notmatch [regex]::Escape($route)) {
        Add-RuleError `
            'backend/src/main/java/com/matrimony/auth/AuthController.java' `
            1 `
            "required auth endpoint $route is missing"
    }
}

$backendJavaRoot = Join-Path $repoRoot 'backend/src/main/java'
$backendResourceRoot = Join-Path $repoRoot 'backend/src/main/resources'

$allJava = Get-ChildItem `
    $backendJavaRoot `
    -Recurse `
    -Filter *.java `
    -ErrorAction SilentlyContinue

$allFiles = @()
$allFiles += Get-ChildItem $backendJavaRoot -Recurse -File -ErrorAction SilentlyContinue
$allFiles += Get-ChildItem $backendResourceRoot -Recurse -File -ErrorAction SilentlyContinue
$allFiles += Get-Item (Join-Path $repoRoot 'backend/build.gradle.kts') -ErrorAction SilentlyContinue
$allFiles += Get-Item (Join-Path $repoRoot 'docker-compose.yml') -ErrorAction SilentlyContinue

$declarations = @{
    'RegisterRequest' = 'duplicate RegisterRequest declaration'
    'LoginRequest' = 'duplicate LoginRequest declaration'
    'AuthRepository' = 'duplicate AuthRepository declaration'
}

foreach ($entry in $declarations.GetEnumerator()) {
    $matches = @(
        $allJava |
        Select-String `
            -Pattern "\b(class|record|interface)\s+$($entry.Key)\b" `
            -ErrorAction SilentlyContinue
    )

    if ($matches.Count -gt 1) {
        foreach ($m in $matches) {
            Add-RuleError `
                ($m.Path.Substring($repoRoot.Length + 1)) `
                $m.LineNumber `
                $entry.Value
        }
    }
}

foreach ($file in $allFiles) {
    $relative = $file.FullName.Substring($repoRoot.Length + 1)
    $lines = Get-Content $file.FullName -ErrorAction SilentlyContinue

    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]

        if ($line.Contains('`r`n')) {
            Add-RuleError $relative ($i + 1) 'literal r`n is present'
        }

        if ($line -match 'hostnameVerifier\s*\{\s*true|TrustManager|trust-all|trustAll') {
            Add-RuleError $relative ($i + 1) 'dangerous TLS/trust-all implementation is present'
        }

        # Only flag actual logging calls that reference credentials.
        if ($line -match '(?i)(System\.out\.println|logger\.(trace|debug|info|warn|error)|log\.(trace|debug|info|warn|error))\s*\([^;]*(authorization|bearer|token)') {
            Add-RuleError $relative ($i + 1) 'credential/token logging is present'
        }

        if ($line -match '(?i)(System\.out\.println|logger\.(trace|debug|info|warn|error)|log\.(trace|debug|info|warn|error))\s*\([^;]*password') {
            Add-RuleError $relative ($i + 1) 'password logging is present'
        }

        if ($line -match '(?i)\b(jwt)\b') {
            Add-RuleError $relative ($i + 1) 'JWT usage is forbidden in Phase 05'
        }

        if ($line -match '(?i)\b(redis|lettuce|jedis)\b') {
            Add-RuleError $relative ($i + 1) 'Redis is forbidden in the local rate limiter'
        }

        if ($line -match '(?i)\bOTP\b') {
            Add-RuleError $relative ($i + 1) 'OTP must remain absent from active authentication'
        }

        if ($line -match '(?i)TODO.*(auth|bypass|security)') {
            Add-RuleError $relative ($i + 1) 'obvious authentication bypass/security TODO is present'
        }
    }
}

$build = Get-Text 'backend/build.gradle.kts'

if ($build -match 'spring-security-oauth2|jjwt|nimbus-jose-jwt') {
    Add-RuleError 'backend/build.gradle.kts' 1 'JWT-oriented dependency is present'
}

if ($build -match '(?i)\b(redis|lettuce|jedis)\b') {
    Add-RuleError 'backend/build.gradle.kts' 1 'Redis dependency is present'
}

$application = Get-Text 'backend/src/main/resources/application.yml'

if ($application -match '(?im)^\s*password:\s*[^\$\s][^\r\n]*$') {
    Add-RuleError `
        'backend/src/main/resources/application.yml' `
        1 `
        'database password must come from an environment variable'
}

$compose = Get-Text 'docker-compose.yml'

if ($compose -match '(?im)^\s*POSTGRES_PASSWORD:\s*[^\$\s][^\r\n]*$') {
    Add-RuleError `
        'docker-compose.yml' `
        1 `
        'PostgreSQL password must come from an environment variable'
}

if ($errors.Count -eq 0) {
    Write-Host 'PHASE 05 STATIC VERIFICATION: PASSED'
    Write-Host 'Checked authentication endpoints, password/session safeguards, rate limiting, forbidden authentication features, secret handling, and migration requirements.'
    exit 0
}

Write-Host "PHASE 05 STATIC VERIFICATION: FAILED"
$errors | ForEach-Object { Write-Host $_ }
exit 1
