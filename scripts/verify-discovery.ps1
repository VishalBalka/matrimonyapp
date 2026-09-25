$ErrorActionPreference = "Stop"

# Phase 11 - Discovery & Search static verification.
# Human-readable output. Exit code 0 = PASS, 1 = FAILURE.
# Every failure prints: file path, line number where practical, violated rule.

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

function Full($relativePath) {
    return Join-Path $root $relativePath
}

function Require-File($relativePath) {
    if (Test-Path (Full $relativePath) -PathType Leaf) {
        Pass "Required file - $relativePath"
        return $true
    }
    Fail $relativePath $null "required-file" "File does not exist."
    return $false
}

# Returns @{ Number = <1-based line>; Text = <line with comments blanked> } for every line.
# Java/Kotlin block comments and // comments are blanked so that explanatory prose can
# neither trigger a false failure nor satisfy a required-pattern check.
function Get-CodeLines($relativePath) {
    $path = Full $relativePath
    $result = @()
    if (-not (Test-Path $path -PathType Leaf)) { return $result }

    $inBlock = $false
    $number = 0
    foreach ($raw in (Get-Content $path)) {
        $number++
        $text = $raw
        $out = ""
        $i = 0
        while ($i -lt $text.Length) {
            if ($inBlock) {
                $end = $text.IndexOf("*/", $i)
                if ($end -lt 0) { $i = $text.Length } else { $inBlock = $false; $i = $end + 2 }
            } elseif ($i + 1 -lt $text.Length -and $text.Substring($i, 2) -eq "/*") {
                $inBlock = $true
                $i += 2
            } elseif ($i + 1 -lt $text.Length -and $text.Substring($i, 2) -eq "//") {
                $i = $text.Length
            } else {
                $out += $text[$i]
                $i++
            }
        }
        $result += [pscustomobject]@{ Number = $number; Text = $out }
    }
    return $result
}

function Check-Present($relativePath, $pattern, $rule, $message) {
    if (-not (Test-Path (Full $relativePath) -PathType Leaf)) {
        Fail $relativePath $null "required-file" "File does not exist."
        return
    }
    foreach ($line in (Get-CodeLines $relativePath)) {
        if ($line.Text -match $pattern) {
            Pass "${relativePath}:$($line.Number) - $message"
            return
        }
    }
    Fail $relativePath $null $rule $message
}

function Check-Absent($relativePath, $pattern, $rule, $message) {
    if (-not (Test-Path (Full $relativePath) -PathType Leaf)) { return }
    $clean = $true
    foreach ($line in (Get-CodeLines $relativePath)) {
        if ($line.Text -match $pattern) {
            Fail $relativePath $line.Number $rule $message
            $clean = $false
        }
    }
    if ($clean) { Pass "$relativePath - $message (absent)" }
}

function Relative($fullName) {
    return $fullName.Substring($root.Length + 1).Replace("\", "/")
}

function Source-Files($relativeDir, $extensions) {
    $dir = Full $relativeDir
    if (-not (Test-Path $dir)) { return @() }
    return @(Get-ChildItem $dir -Recurse -File | Where-Object { $extensions -contains $_.Extension } |
        ForEach-Object { Relative $_.FullName })
}

Write-Host "PHASE 11 DISCOVERY VERIFICATION"

$be = "backend/src/main/java/com/matrimony"
$ad = "android/app/src/main/java/com/matrimonyapp"

# ---------------------------------------------------------------------------
# 1. Required files
# ---------------------------------------------------------------------------
$requiredFiles = @(
    "$be/discovery/DiscoveryController.java",
    "$be/discovery/DiscoveryService.java",
    "$be/discovery/DiscoveryRepository.java",
    "$be/discovery/DiscoveryValidator.java",
    "$be/discovery/DiscoveryModels.java",
    "backend/src/test/java/com/matrimony/discovery/DiscoveryValidatorTest.java",
    "backend/src/test/java/com/matrimony/discovery/DiscoveryServiceTest.java",
    "backend/src/test/java/com/matrimony/discovery/DiscoveryControllerTest.java",
    "backend/src/test/java/com/matrimony/discovery/DiscoveryPhotoAccessTest.java",
    "backend/src/test/java/com/matrimony/discovery/DiscoveryRepositoryIntegrationTest.java",
    "$ad/data/remote/ProfileSearchApi.kt",
    "$ad/data/remote/ProfileSearchDtos.kt",
    "$ad/data/discovery/ProfileSearch.kt",
    "$ad/data/discovery/ProfileSearchRepository.kt",
    "$ad/ui/search/SearchScreen.kt",
    "$ad/ui/search/ProfileDetailScreen.kt",
    "android/app/src/test/java/com/matrimonyapp/data/discovery/ProfileSearchRepositoryTest.kt",
    "android/app/src/test/java/com/matrimonyapp/data/discovery/SearchFiltersTest.kt",
    "android/app/src/test/java/com/matrimonyapp/data/discovery/SearchStateTest.kt",
    "android/app/src/test/java/com/matrimonyapp/data/remote/ProfileSearchDtosTest.kt",
    "android/app/src/test/java/com/matrimonyapp/data/remote/ApiClientAuthInterceptorTest.kt",
    "docs/handoff/phase-11-discovery-search.md"
)
foreach ($file in $requiredFiles) { [void](Require-File $file) }

# ---------------------------------------------------------------------------
# 2. Search API exists and requires authentication
# ---------------------------------------------------------------------------
$controller = "$be/discovery/DiscoveryController.java"
Check-Present $controller '@RequestMapping\("/api/v1/profiles"\)' "search-api" "Discovery controller is mapped under /api/v1/profiles."
Check-Present $controller '@GetMapping\("/search"\)' "search-api" "GET /api/v1/profiles/search endpoint exists."
Check-Present $controller 'AuthAuthenticationFilter\.PRINCIPAL_ATTRIBUTE' "search-auth" "Search API reads the authenticated principal set by the existing filter."
Check-Present $controller 'HttpStatus\.UNAUTHORIZED' "search-auth" "Search API answers HTTP 401 when no principal is present."
Check-Absent  $controller '@RequestParam' "no-client-query-fragments" "Search parameters must be validated through DiscoveryValidator, not bound directly."

# The shared authentication filter must not exempt any discovery path.
$filterPath = "$be/auth/AuthAuthenticationFilter.java"
$filterOk = $true
foreach ($line in (Get-CodeLines $filterPath)) {
    if ($line.Text -match 'shouldNotFilter|equals\("/api/') {
        if ($line.Text -match '/api/v1/profiles|/api/v1/profile"|startsWith') {
            Fail $filterPath $line.Number "search-auth" "Authentication filter exempts a profile/discovery path or uses a prefix exemption."
            $filterOk = $false
        }
    }
}
if ($filterOk) { Pass "Authentication filter does not exempt discovery paths" }

# No second authentication mechanism.
foreach ($file in (Source-Files "$be/discovery" @(".java"))) {
    Check-Absent $file 'Authorization|Bearer|Basic |ApiKey|api[-_]key' "single-auth-mechanism" "Discovery code must not implement its own authentication."
}

# ---------------------------------------------------------------------------
# 3. DTOs do not expose private or authentication data
# ---------------------------------------------------------------------------
$forbiddenFields = '(?i)\b(phone|phoneNumber|password|passwordHash|token|tokenHash|sessionToken|photoStorageKey|storageKey|storagePath|filePath|filesystemPath|userId|email|dateOfBirth|bio)\b'
Check-Absent "$be/discovery/DiscoveryModels.java" $forbiddenFields "dto-private-data" "Discovery response DTOs must not expose phone, credentials, session data, storage details, user id, email or date of birth."
Check-Absent "$ad/data/remote/ProfileSearchDtos.kt" $forbiddenFields "dto-private-data" "Android discovery DTOs must not carry phone, credentials, session data, storage details, user id, email or date of birth."
Check-Absent "$ad/data/discovery/ProfileSearch.kt" $forbiddenFields "dto-private-data" "Android discovery domain models must not carry private or internal fields."

# Persistence/owner models must not be reused as discovery responses.
Check-Absent "$be/discovery/DiscoveryController.java" 'ProfileModels\.ProfileResponse|ProfileResponse\b' "dto-separation" "Controller must return dedicated discovery DTOs, not the owner-facing profile model."
Check-Absent "$be/discovery/DiscoveryService.java" 'ProfileModels\.' "dto-separation" "Discovery service must not build responses from owner-facing profile models."

# ---------------------------------------------------------------------------
# 4. Visibility, own-profile exclusion, database-level pagination
# ---------------------------------------------------------------------------
$repo = "$be/discovery/DiscoveryRepository.java"
Check-Present $repo 'user_id\s*<>\s*\?' "own-profile-exclusion" "Own-profile exclusion is part of the eligibility predicate."
Check-Present $repo 'country IS NOT NULL AND state_province IS NOT NULL' "visibility-rule" "Minimum discovery eligibility rule (complete location) is enforced in SQL."
Check-Present "$be/discovery/DiscoveryService.java" 'principal\.userId\(\)' "own-profile-exclusion" "Viewer id comes from the authenticated principal, never from the request."
Check-Present $repo 'LIMIT \? OFFSET \?' "db-pagination" "Pagination is performed in SQL with LIMIT/OFFSET."
Check-Present $repo 'ORDER BY created_at DESC, id ASC' "deterministic-order" "Ordering is deterministic with a unique tiebreaker."
Check-Present $repo 'SELECT COUNT\(\*\)' "db-pagination" "Total count is computed by the database."
Check-Present $repo 'date_of_birth\s*<=\s*\?' "age-from-dob" "Minimum age is derived from date_of_birth."
Check-Present $repo 'date_of_birth\s*>\s*\?' "age-from-dob" "Maximum age is derived from date_of_birth."
Check-Absent  $repo '\.subList\(|\.skip\(|\bstream\(\)[^;]*\.limit\(' "db-pagination" "Results must not be paginated in memory."
Check-Absent  "$be/discovery/DiscoveryService.java" '\.subList\(|\.skip\(|\bstream\(\)[^;]*\.limit\(' "db-pagination" "Results must not be paginated in memory."
Check-Absent  $repo '(?i)\bage\s+(integer|int|smallint)|ADD COLUMN\s+age\b' "age-not-stored" "Age must not be persisted."

# All client values must be bound parameters: no SQL fragment may be built from criteria/query values.
Check-Absent $repo 'append\([^;]*(query|criteria|request|param)\w*\.(country|state|city|page|pageSize|limit|offset)\(\)' "sql-injection" "Client-supplied values must never be concatenated into SQL."
Check-Absent $repo 'append\("[^"]*"\s*\+' "sql-injection" "SQL fragments must be fixed literals."
Check-Absent $repo '(?i)ORDER BY\s+"\s*\+|ORDER BY\s+\?' "sql-injection" "Sort expressions must never come from the client."

# ---------------------------------------------------------------------------
# 5. pageSize limit and parameter allow-list
# ---------------------------------------------------------------------------
$validator = "$be/discovery/DiscoveryValidator.java"
Check-Present $validator 'MAX_PAGE_SIZE\s*=\s*50' "page-size-limit" "Server-side maximum page size is defined."
Check-Present $validator 'Math\.min\(\s*pageSize\s*,\s*MAX_PAGE_SIZE' "page-size-limit" "Oversized page sizes are bounded."
Check-Present $validator 'MAX_PAGE\s*=' "page-limit" "Unreasonable page numbers are bounded."
Check-Present $validator 'ALLOWED_PARAMETERS' "parameter-allow-list" "Only allow-listed query parameters are accepted."
Check-Present $validator 'Unsupported search parameter' "parameter-allow-list" "Unknown parameters (e.g. sort expressions) are rejected."

# ---------------------------------------------------------------------------
# 6. Photo access
# ---------------------------------------------------------------------------
$photo = "$be/profile/ProfilePhotoService.java"
Check-Present $photo 'STORAGE_KEY\s*=\s*Pattern\.compile' "photo-key-format" "Photo storage keys are validated against a strict generated-key format."
Check-Present $photo 'NOFOLLOW_LINKS' "photo-symlink" "Photo reads do not follow symbolic links."
Check-Present $photo 'getParent\(\)\.equals\(storageDirectory\)' "photo-containment" "Resolved photo paths must stay inside the storage directory."
Check-Present "$be/discovery/DiscoveryService.java" 'findDiscoverablePhotoKey' "photo-authorization" "Photo key is resolved from the database with an eligibility check."
Check-Present "$be/discovery/DiscoveryService.java" 'photoService\.readStored' "photo-authorization" "Discovery photos are read through the existing photo service."
foreach ($file in (Source-Files "$be/discovery" @(".java"))) {
    Check-Absent $file 'java\.io\.File|FileInputStream|FileReader|java\.nio\.file|Paths\.get|Path\.of|Files\.' "filesystem-access" "Discovery code must not touch the filesystem directly."
    Check-Absent $file 'getPhotoStorageKey|photo_storage_key\s*,|photoStorageKey' "storage-key-leak" "Discovery code must not expose storage keys."
}
Check-Absent "$be/discovery/DiscoveryModels.java" 'storage|Path' "photo-leak" "Discovery responses must not carry storage details or paths."

# ---------------------------------------------------------------------------
# 7. Android: UI, filters, detail, layering
# ---------------------------------------------------------------------------
$screen = "$ad/ui/search/SearchScreen.kt"
$authScreen = "$ad/ui/auth/AuthenticatedScreen.kt"
Check-Absent  $authScreen 'PlaceholderTab\("Search"\)' "search-placeholder" "The Search placeholder must be replaced."
Check-Present $authScreen 'SearchScreen\(' "search-ui" "Authenticated shell hosts the Search screen."
foreach ($label in @("Country", "State / Province", "City", "Min age", "Max age", "Clear filters")) {
    $escaped = [regex]::Escape($label)
    Check-Present $screen "Text\(""$escaped""" "search-filters" "Search UI provides the '$label' control."
}
Check-Present $screen 'Text\("Search"\)' "search-filters" "Search UI provides a Search action."
Check-Present $screen 'SearchPhase\.INITIAL' "search-states" "Initial state is handled."
Check-Present $screen 'SearchPhase\.LOADING' "search-states" "Loading state is handled."
Check-Present $screen 'SearchPhase\.RESULTS' "search-states" "Results state is handled."
Check-Present $screen 'SearchPhase\.EMPTY' "search-states" "Empty state is handled."
Check-Present $screen 'SearchPhase\.ERROR' "search-states" "Error state is handled."
Check-Present $screen 'onClick\s*=\s*onRetry|onRetry\s*=' "search-retry" "Error state offers a retry action."
Check-Present $screen 'isAuthenticationFailure\(\)' "auth-expiration" "401 is routed through the existing authentication-expiration callback."
Check-Present $screen 'selectedProfileId' "profile-detail" "Tapping a result opens the profile detail screen."

$detail = "$ad/ui/search/ProfileDetailScreen.kt"
Check-Present $detail 'repository\.getProfile' "profile-detail" "Profile detail loads through the discovery repository."
Check-Absent  $detail '(?i)phone' "detail-private-phone" "Profile detail must never display a phone number."
Check-Absent  $detail 'profileId\)?\s*,\s*color|Text\(\s*current\.profileId' "detail-internal-id" "Profile detail must not display internal identifiers."
Check-Present $detail 'Retry' "search-retry" "Profile detail offers a retry action on error."

# Networking must stay out of Compose UI.
foreach ($file in (Source-Files "$ad/ui/search" @(".kt"))) {
    Check-Absent $file 'import retrofit2|import okhttp3|ApiClient|Retrofit|OkHttp|HttpURLConnection|URL\(' "ui-networking" "Compose UI must not contain networking code."
}
Check-Absent "$ad/data/discovery/ProfileSearchRepository.kt" 'tokenStore|TokenStore|\.clear\(\)' "single-token-clear" "Token clearing must stay in ApiClient; the repository must not duplicate it."
Check-Present "$ad/data/discovery/ProfileSearchRepository.kt" 'ApiErrorMapper\.fromResponse' "error-mapping" "Repository reuses ApiErrorMapper for HTTP errors."
Check-Present "$ad/data/discovery/ProfileSearchRepository.kt" 'ApiErrorMapper\.fromThrowable' "error-mapping" "Repository reuses ApiErrorMapper for transport errors."
Check-Absent  "$ad/data/discovery/ProfileSearchRepository.kt" '==\s*401|code\(\)\s*==\s*40[19]' "error-mapping" "HTTP 401/409 mapping must not be duplicated outside ApiErrorMapper."
Check-Present "$ad/data/remote/ApiClient.kt" 'createProfileSearchApi' "api-client" "Search API is created through the shared ApiClient."
Check-Present "$ad/data/remote/ApiClient.kt" 'response\.code == 401' "auth-expiration" "Shared ApiClient still clears authentication on 401."
Check-Present "$ad/data/remote/ProfileSearchApi.kt" 'api/v1/profiles/search' "api-path" "Android search API targets the authenticated search endpoint."

# Bounded photo decoding for untrusted images from other members.
Check-Present "$ad/ui/search/PhotoDecoding.kt" 'inJustDecodeBounds' "photo-decoding" "Photos are size-checked before decoding."
Check-Present "$ad/ui/search/PhotoDecoding.kt" 'inSampleSize' "photo-decoding" "Photos are down-sampled while decoding."

# ---------------------------------------------------------------------------
# 8. Forbidden features must not have been introduced
# ---------------------------------------------------------------------------
$forbiddenFeatures = '(?i)\bOTP\b|one[- ]time password|SMS auth|social login|google sign|facebook sign|\bMFA\b|multi[- ]factor|two[- ]factor|password[- ]?reset|reset[- ]?password|forgot[- ]?password'
foreach ($dir in @("$be/discovery", "$ad/ui/search", "$ad/data/discovery")) {
    foreach ($file in (Source-Files $dir @(".java", ".kt"))) {
        Check-Absent $file $forbiddenFeatures "forbidden-auth-mechanism" "OTP, SMS, social login, MFA or password reset must not be introduced."
    }
}
$outOfScope = '(?i)\b(chat|subscription|payment|push[ _-]?notification|recommendation|matchmaking)\w*|\b(like|match)(Repository|Service|Controller|Api)\b'
foreach ($dir in @("$be/discovery", "$ad/data/discovery")) {
    foreach ($file in (Source-Files $dir @(".java", ".kt"))) {
        Check-Absent $file $outOfScope "out-of-scope-feature" "Chat, likes, matches, subscriptions, payments, push notifications and recommendations are out of scope for Phase 11."
    }
}
foreach ($file in (Source-Files "$be/discovery" @(".java"))) {
    Check-Absent $file '(?i)\b(log|logger)\.\w+\(' "sensitive-logging" "Discovery code must not log; profile data and credentials must never reach logs."
}
foreach ($file in (Source-Files "$ad/ui/search" @(".kt")) + (Source-Files "$ad/data/discovery" @(".kt"))) {
    Check-Absent $file '\bLog\.[dviwe]\(|println\(|Timber' "sensitive-logging" "Android discovery code must not log."
}

# ---------------------------------------------------------------------------
# 9. Android networking must not have been weakened
# ---------------------------------------------------------------------------
$mainManifest = Full "android/app/src/main/AndroidManifest.xml"
if (Test-Path $mainManifest) {
    $manifestText = Get-Content $mainManifest -Raw
    if ($manifestText -match 'usesCleartextTraffic="true"') {
        Fail "android/app/src/main/AndroidManifest.xml" $null "cleartext-global" "Manifest globally enables cleartext traffic."
    } elseif ($manifestText -match 'networkSecurityConfig=') {
        Fail "android/app/src/main/AndroidManifest.xml" $null "cleartext-release" "Release manifest must not carry the debug network-security override."
    } else {
        Pass "Release manifest has no global cleartext permission and no network-security override"
    }
}
$netConfigRel = "android/app/src/debug/res/xml/network_security_config.xml"
$netConfig = Full $netConfigRel
if (Test-Path $netConfig) {
    $netText = Get-Content $netConfig -Raw
    if ($netText -match '<base-config[^>]*cleartextTrafficPermitted="false"') {
        Pass "Debug network config keeps cleartext denied by default"
    } else {
        Fail $netConfigRel $null "cleartext-global" "base-config must keep cleartextTrafficPermitted=false."
    }
    $domains = @([regex]::Matches($netText, '<domain[^>]*>([^<]+)</domain>') | ForEach-Object { $_.Groups[1].Value.Trim() })
    $unexpected = @($domains | Where-Object { $_ -notin @("10.0.2.2", "192.168.29.82") })
    if ($unexpected.Count -eq 0) {
        Pass "Debug cleartext is limited to the approved local hosts"
    } else {
        Fail $netConfigRel $null "cleartext-hosts" "Unapproved cleartext host(s): $($unexpected -join ', ')."
    }
} else {
    Fail $netConfigRel $null "required-file" "Debug network security configuration is missing."
}
foreach ($file in (Source-Files "android/app/src/main" @(".kt", ".xml"))) {
    if ($file -like "*ApiClient.kt") { continue }
    Check-Absent $file 'http://' "cleartext-url" "Cleartext http:// URLs must not be introduced outside the approved API client."
}

# ---------------------------------------------------------------------------
# 10. Source integrity and migrations
# ---------------------------------------------------------------------------
# Literal PowerShell newline-escape corruption (backtick-r backtick-n) in source files.
$corrupt = '`' + 'r' + '`' + 'n'
$integrityDirs = @("backend/src", "android/app/src", "docs")
$integrityExt = @(".java", ".kt", ".sql", ".yml", ".yaml", ".md", ".xml")
foreach ($dir in $integrityDirs) {
    foreach ($file in (Source-Files $dir $integrityExt)) {
        $lines = @(Get-Content (Full $file))
        for ($i = 0; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Contains($corrupt)) {
                Fail $file ($i + 1) "source-integrity" "Literal PowerShell newline escape found."
            }
        }
    }
}
Pass "Source integrity scan completed (backend/src, android/app/src, docs)"

$migrationDir = Full "backend/src/main/resources/db/migration"
$files = @(Get-ChildItem $migrationDir -File -Filter "V*.sql")
$versions = @()
foreach ($f in $files) {
    if ($f.Name -match '^V([0-9]+)__.+\.sql$') {
        $versions += [int]$Matches[1]
    } else {
        Fail "backend/src/main/resources/db/migration/$($f.Name)" $null "migration-naming" "Migration does not use V<number>__description.sql."
    }
}
$dups = @($versions | Group-Object | Where-Object { $_.Count -gt 1 })
if ($dups.Count -eq 0) {
    Pass "No duplicate migration versions"
} else {
    foreach ($d in $dups) {
        Fail "backend/src/main/resources/db/migration" $null "duplicate-version" "Migration version V$($d.Name) appears more than once."
    }
}

# Already-applied migrations V1..V5 must be byte-for-byte unchanged relative to the committed baseline.
$git = Get-Command git -ErrorAction SilentlyContinue
if ($git) {
    Push-Location $root
    try {
        $changed = @(& git diff --name-only HEAD -- "backend/src/main/resources/db/migration/V1__initial_auth_schema.sql" "backend/src/main/resources/db/migration/V2__auth_timestamps.sql" "backend/src/main/resources/db/migration/V3__profile_schema.sql" "backend/src/main/resources/db/migration/V4__profile_enrichment.sql" "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql" 2>$null)
    } finally {
        Pop-Location
    }
    if ($changed.Count -eq 0) {
        Pass "Applied migrations V1 through V5 are unmodified"
    } else {
        foreach ($c in $changed) { Fail $c $null "migration-immutable" "Already-applied migration was modified." }
    }
} else {
    Write-Host "SKIP: git not available - V1 through V5 immutability was not checked"
}

# ---------------------------------------------------------------------------
# 11. Phase 10 verification scripts are preserved
# ---------------------------------------------------------------------------
foreach ($script in @("verify.ps1", "verify-security.ps1", "verify-migrations.ps1", "verify-android.ps1")) {
    $path = Full "scripts/$script"
    if ((Test-Path $path) -and ((Get-Content $path -Raw) -match "PHASE 10")) {
        Pass "Phase 10 script preserved - scripts/$script"
    } else {
        Fail "scripts/$script" $null "phase-10-scripts" "Phase 10 verification script is missing or was replaced."
    }
}

if ($failures -eq 0) {
    Write-Host "PHASE 11 DISCOVERY VERIFICATION: PASS"
    exit 0
}

Write-Host "PHASE 11 DISCOVERY VERIFICATION: FAIL ($failures failure(s))"
exit 1
