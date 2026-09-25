param(
    [string]$BaseUrl = "http://localhost:8080",
    # Optional: a second backend instance started against the same database with a very short
    # session TTL (for example auth.session.ttl=2s). Needed only for the session-expiry check.
    [string]$ShortTtlBaseUrl = ""
)

$ErrorActionPreference = "Stop"

# Phase 11 runtime HTTP verification against a RUNNING local backend.
# - Creates throwaway users (@example.test) with random passwords generated at runtime.
# - Never prints passwords, tokens, Authorization headers or response bodies.
# - Test data cannot be deleted through the API; it is isolated by a per-run marker in the
#   country/state names so it never matches or disturbs real profiles.
# Exit code 0 = every executed check passed, 1 = at least one failed.

$script:failures = 0
$script:passes = 0
$script:skips = 0
$isPs7 = $PSVersionTable.PSVersion.Major -ge 7

function Check($condition, $label) {
    if ($condition) { $script:passes++; Write-Host "PASS: $label" }
    else { $script:failures++; Write-Host "FAIL: $label" }
}

function Skip($label, $reason) {
    $script:skips++
    Write-Host "SKIP: $label - $reason"
}

# Returns @{ Status = <int>; Raw = <string>; Json = <object or $null>; ContentType = <string> }
function Call($base, $method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }
    $arguments = @{
        Uri = "$base$path"; Method = $method; Headers = $headers
        UseBasicParsing = $true; TimeoutSec = 20
    }
    if ($null -ne $body) {
        $arguments["Body"] = ($body | ConvertTo-Json -Compress)
        $arguments["ContentType"] = "application/json"
    }
    $status = 0; $raw = ""; $contentType = ""
    try {
        $response = Invoke-WebRequest @arguments
        $status = [int]$response.StatusCode
        $raw = [string]$response.Content
        $contentType = [string]$response.Headers["Content-Type"]
    } catch {
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
                $raw = [string]$_.ErrorDetails.Message
            }
        } else {
            throw
        }
    }
    $json = $null
    if ($raw -and $raw.TrimStart().StartsWith("{")) {
        try { $json = $raw | ConvertFrom-Json } catch { $json = $null }
    }
    return @{ Status = $status; Raw = $raw; Json = $json; ContentType = $contentType }
}

function Search($token, $query) {
    $pairs = @()
    foreach ($key in $query.Keys) {
        $pairs += ("{0}={1}" -f [uri]::EscapeDataString($key), [uri]::EscapeDataString([string]$query[$key]))
    }
    $suffix = ""
    if ($pairs.Count -gt 0) { $suffix = "?" + ($pairs -join "&") }
    return Call $BaseUrl "GET" "/api/v1/profiles/search$suffix" $token $null
}

function Names($result) {
    if ($null -eq $result.Json -or $null -eq $result.Json.items) { return @() }
    return @($result.Json.items | ForEach-Object { $_.displayName } | Sort-Object)
}

function Register-And-Login($base, $name, $password) {
    $email = "$($name.ToLower())@example.test"
    $register = Call $base "POST" "/api/v1/auth/register" $null @{
        displayName = $name; email = $email; password = $password; confirmPassword = $password
    }
    if ($register.Status -ne 201 -and $register.Status -ne 200 -and $register.Status -ne 409) {
        throw "Registration for $name returned HTTP $($register.Status)."
    }
    $login = Call $base "POST" "/api/v1/auth/login" $null @{ email = $email; password = $password }
    if ($login.Status -ne 200 -or -not $login.Json.token) {
        throw "Login for $name returned HTTP $($login.Status)."
    }
    return [string]$login.Json.token
}

function Dob($years, $extraDays) {
    return (Get-Date).Date.AddYears(-$years).AddDays(-$extraDays).ToString("yyyy-MM-dd")
}

function Put-Profile($token, $name, $dob, $gender, $country, $state, $city, $phone) {
    return Call $BaseUrl "PUT" "/api/v1/profile" $token @{
        displayName = $name; dateOfBirth = $dob; gender = $gender; country = $country
        stateProvince = $state; city = $city; bio = "Phase 11 runtime check"; phoneNumber = $phone
    }
}

Write-Host "PHASE 11 RUNTIME HTTP VERIFICATION"
Write-Host "Target: $BaseUrl"

$health = Call $BaseUrl "GET" "/api/health" $null $null
Check ($health.Status -eq 200) "Backend is reachable (GET /api/health -> 200)"
if ($health.Status -ne 200) {
    Write-Host "PHASE 11 RUNTIME HTTP VERIFICATION: FAIL (backend not reachable)"
    exit 1
}

$run = [guid]::NewGuid().ToString("N").Substring(0, 8)
$password = "Rt!" + [guid]::NewGuid().ToString("N").Substring(0, 16)   # never printed
$country = "RtCountry-$run"
$state = "RtState-$run"
$otherState = "RtStateOther-$run"
$otherCountry = "RtOther-$run"
$secretPhone = "+91 98765 43210"

# ---------------------------------------------------------------------------
# Unauthorized access
# ---------------------------------------------------------------------------
$noAuth = Search $null @{ country = $country }
Check ($noAuth.Status -eq 401) "Search without credentials -> 401"
$badToken = Search "not-a-real-session-token" @{ country = $country }
Check ($badToken.Status -eq 401) "Search with an invalid session token -> 401"

# ---------------------------------------------------------------------------
# Data setup: viewer A plus B, C, D
# ---------------------------------------------------------------------------
$tokenA = Register-And-Login $BaseUrl "RtA-$run" $password
$tokenB = Register-And-Login $BaseUrl "RtB-$run" $password
$tokenC = Register-And-Login $BaseUrl "RtC-$run" $password
$tokenD = Register-And-Login $BaseUrl "RtD-$run" $password

$setup = @(
    (Put-Profile $tokenA "RtA-$run" (Dob 30 10) "FEMALE" $country $state "RtCityA" "+91 90000 11111"),
    (Put-Profile $tokenB "RtB-$run" (Dob 26 30) "MALE"   $country $state "RtCityB" $secretPhone),
    (Put-Profile $tokenC "RtC-$run" (Dob 29 30) "MALE"   $otherCountry $state "RtCityC" "+91 90000 33333"),
    (Put-Profile $tokenD "RtD-$run" (Dob 34 30) "MALE"   $country $otherState "RtCityD" "+91 90000 44444")
)
Check (@($setup | Where-Object { $_.Status -ne 200 }).Count -eq 0) "Test profiles created for four users"

# ---------------------------------------------------------------------------
# Authenticated search, own-profile exclusion, filters
# ---------------------------------------------------------------------------
$byCountry = Search $tokenA @{ country = $country }
Check ($byCountry.Status -eq 200) "Authenticated search -> 200"
Check ((Names $byCountry) -join "," -eq "RtB-$run,RtD-$run") "Country filter returns only matching other members"
Check ((Names $byCountry) -notcontains "RtA-$run") "Own profile is excluded from results"
Check ($byCountry.Json.totalItems -eq 2) "totalItems reflects the filtered count"

Check ((Names (Search $tokenA @{ state = $state })) -join "," -eq "RtB-$run,RtC-$run") "State filter matches across countries and excludes the viewer"
Check ((Names (Search $tokenA @{ city = "rtcityb" })) -join "," -eq "RtB-$run") "City filter is case-insensitive"
Check ((Names (Search $tokenA @{ country = $country; minAge = 30 })) -join "," -eq "RtD-$run") "Minimum age filter"
Check ((Names (Search $tokenA @{ country = $country; maxAge = 30 })) -join "," -eq "RtB-$run") "Maximum age filter"
Check ((Names (Search $tokenA @{ country = $country; state = $state; minAge = 25; maxAge = 30 })) -join "," -eq "RtB-$run") "Combined filters use AND semantics"
Check ((Names (Search $tokenA @{ country = $country; state = $otherState; maxAge = 30 })).Count -eq 0) "Combined filters with no overlap return nothing"

# ---------------------------------------------------------------------------
# Empty result, pagination, page-size bound
# ---------------------------------------------------------------------------
$empty = Search $tokenA @{ city = "NoSuchCity-$run" }
Check ($empty.Status -eq 200 -and @($empty.Json.items).Count -eq 0 -and $empty.Json.totalItems -eq 0) "Empty result is HTTP 200 with zero items"

$page0 = Search $tokenA @{ country = $country; pageSize = 1; page = 0 }
$page1 = Search $tokenA @{ country = $country; pageSize = 1; page = 1 }
$page2 = Search $tokenA @{ country = $country; pageSize = 1; page = 2 }
Check (@($page0.Json.items).Count -eq 1 -and @($page1.Json.items).Count -eq 1) "Pagination returns one item on each of the first two pages"
Check (@($page2.Json.items).Count -eq 0 -and $page2.Status -eq 200) "A page beyond the end is an empty 200"
Check ($page0.Json.items[0].profileId -ne $page1.Json.items[0].profileId) "Pages do not repeat a profile"
Check ($page0.Json.totalPages -eq 2 -and $page0.Json.totalItems -eq 2) "Pagination totals are correct"

$big = Search $tokenA @{ country = $country; pageSize = 1000 }
Check ($big.Status -eq 200 -and $big.Json.pageSize -eq 50) "pageSize above the maximum is bounded to 50"

# ---------------------------------------------------------------------------
# Invalid input and method handling
# ---------------------------------------------------------------------------
Check ((Search $tokenA @{ minAge = "abc" }).Status -eq 400) "Non-numeric minAge -> 400"
Check ((Search $tokenA @{ minAge = 40; maxAge = 30 }).Status -eq 400) "minAge greater than maxAge -> 400"
Check ((Search $tokenA @{ page = -1 }).Status -eq 400) "Negative page -> 400"
Check ((Search $tokenA @{ pageSize = 0 }).Status -eq 400) "pageSize 0 -> 400"
Check ((Search $tokenA @{ sort = "display_name" }).Status -eq 400) "Client-supplied sort is rejected -> 400"
$wrongMethod = Call $BaseUrl "POST" "/api/v1/profiles/search" $tokenA $null
Check ($wrongMethod.Status -eq 405) "POST to search -> 405 (not 500)"

# ---------------------------------------------------------------------------
# Private data must never appear
# ---------------------------------------------------------------------------
$leakScan = Search $tokenA @{ country = $country }
Check ($leakScan.Raw -notmatch "98765" -and $leakScan.Raw -notmatch "(?i)phone") "Private phone number is absent from search results"
Check ($leakScan.Raw -notmatch "(?i)password|hash|token|storage|userId|dateOfBirth|email") "No credential, storage, user-id, DOB or email fields in search results"

$bSummary = $byCountry.Json.items | Where-Object { $_.displayName -eq "RtB-$run" } | Select-Object -First 1
$detail = Call $BaseUrl "GET" "/api/v1/profiles/$($bSummary.profileId)" $tokenA $null
Check ($detail.Status -eq 200 -and $detail.Json.displayName -eq "RtB-$run") "Profile detail of another member -> 200"
Check ($detail.Raw -notmatch "98765" -and $detail.Raw -notmatch "(?i)phone") "Private phone number is absent from profile detail"
Check ((Call $BaseUrl "GET" "/api/v1/profiles/$($bSummary.profileId)" $null $null).Status -eq 401) "Profile detail without credentials -> 401"

$own = Call $BaseUrl "GET" "/api/v1/profile" $tokenA $null
Check ((Call $BaseUrl "GET" "/api/v1/profiles/$($own.Json.profileId)" $tokenA $null).Status -eq 404) "Own profile cannot be resolved through discovery detail -> 404"
Check ((Call $BaseUrl "GET" "/api/v1/profiles/not-a-uuid" $tokenA $null).Status -eq 400) "Malformed profile id -> 400"
Check ((Call $BaseUrl "GET" "/api/v1/profiles/$([guid]::NewGuid())" $tokenA $null).Status -eq 404) "Unknown profile id -> 404"

# ---------------------------------------------------------------------------
# Photos (multipart upload needs PowerShell 7 -Form)
# ---------------------------------------------------------------------------
if ($isPs7) {
    $tmp = Join-Path ([System.IO.Path]::GetTempPath()) "rt-$run.jpg"
    [System.IO.File]::WriteAllBytes($tmp, [byte[]](0xFF, 0xD8, 0xFF, 0xE0, 1, 2, 3, 4, 5, 6))
    try {
        $upload = Invoke-WebRequest -Uri "$BaseUrl/api/v1/profile/photo" -Method Post -UseBasicParsing `
            -Headers @{ Authorization = "Bearer $tokenB" } -Form @{ photo = (Get-Item $tmp) } -SkipHttpErrorCheck
        Check ([int]$upload.StatusCode -eq 200 -or [int]$upload.StatusCode -eq 201) "Test photo uploaded for member B"
    } finally {
        Remove-Item $tmp -ErrorAction SilentlyContinue
    }
    $withPhoto = Search $tokenA @{ country = $country }
    $bItem = $withPhoto.Json.items | Where-Object { $_.displayName -eq "RtB-$run" } | Select-Object -First 1
    Check ($bItem.photoAvailable -eq $true -and $bItem.photoUrl -eq "/api/v1/profiles/$($bItem.profileId)/photo") "Search reports photo availability with an authorized route, not a path"
    Check ($withPhoto.Raw -notmatch "\.jpg|\.png|profile-photos|\\\\") "No storage key or filesystem path in search results"
    $photo = Call $BaseUrl "GET" "/api/v1/profiles/$($bItem.profileId)/photo" $tokenA $null
    Check ($photo.Status -eq 200 -and $photo.ContentType -like "image/jpeg*") "Discovery photo served to an authenticated member"
    Check ((Call $BaseUrl "GET" "/api/v1/profiles/$($bItem.profileId)/photo" $null $null).Status -eq 401) "Discovery photo without credentials -> 401"
    Check ((Call $BaseUrl "GET" "/api/v1/profiles/$($own.Json.profileId)/photo" $tokenA $null).Status -eq 404) "Own photo is not served through the discovery route"
} else {
    Skip "Photo upload and discovery photo checks" "requires PowerShell 7 (-Form); run with pwsh"
}

# ---------------------------------------------------------------------------
# Session revocation and expiry
# ---------------------------------------------------------------------------
$loginAgain = Call $BaseUrl "POST" "/api/v1/auth/login" $null @{ email = "rta-$run@example.test"; password = $password }
$revocable = [string]$loginAgain.Json.token
Check ((Search $revocable @{ country = $country }).Status -eq 200) "Second session can search before logout"
[void](Call $BaseUrl "POST" "/api/v1/auth/logout" $revocable $null)
Check ((Search $revocable @{ country = $country }).Status -eq 401) "Search with a logged-out (revoked) session -> 401"

if ($ShortTtlBaseUrl) {
    $shortToken = Register-And-Login $ShortTtlBaseUrl "RtA-$run" $password
    $immediate = Call $ShortTtlBaseUrl "GET" "/api/v1/profiles/search?country=$([uri]::EscapeDataString($country))" $shortToken $null
    Check ($immediate.Status -eq 200) "Short-TTL session can search immediately after login"
    Start-Sleep -Seconds 5
    $expired = Call $ShortTtlBaseUrl "GET" "/api/v1/profiles/search?country=$([uri]::EscapeDataString($country))" $shortToken $null
    Check ($expired.Status -eq 401) "Search with an expired session -> 401"
} else {
    Skip "Expired-session check" "start a second backend with a ~2s session TTL and pass -ShortTtlBaseUrl"
}

Write-Host "Executed: $($script:passes) passed, $($script:failures) failed, $($script:skips) skipped."
if ($script:failures -eq 0) {
    Write-Host "PHASE 11 RUNTIME HTTP VERIFICATION: PASS (executed checks)"
    exit 0
}
Write-Host "PHASE 11 RUNTIME HTTP VERIFICATION: FAIL"
exit 1
