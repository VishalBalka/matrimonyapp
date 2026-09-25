$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$androidSrc = Join-Path $root 'android/app/src'
$backendSrc = Join-Path $root 'backend/src'
$script:checks = @()

function Add-Check($name, $passed, $detail) {
    $script:checks += [pscustomobject]@{ Name = $name; Passed = $passed; Detail = $detail }
}

function Require-File($path) {
    $full = Join-Path $root $path
    if (-not (Test-Path $full -PathType Leaf)) {
        Add-Check "Required file" $false "$path"
        return $false
    }
    Add-Check "Required file" $true "$path"
    return $true
}

$required = @(
    'backend/src/main/resources/db/migration/V4__profile_enrichment.sql',
    'backend/src/main/java/com/matrimony/profile/ProfilePhotoService.java',
    'backend/src/main/java/com/matrimony/profile/ProfileController.java',
    'backend/src/main/java/com/matrimony/profile/ProfileRepository.java',
    'android/app/src/main/java/com/matrimonyapp/data/remote/ProfileApi.kt',
    'android/app/src/main/java/com/matrimonyapp/data/profile/ProfileRepository.kt',
    'android/app/src/main/java/com/matrimonyapp/data/profile/ProfileValidation.kt',
    'android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt',
    'backend/src/test/java/com/matrimony/profile/ProfilePhotoServiceTest.java',
    'scripts/verify-phase-08.ps1'
)
foreach ($file in $required) { [void](Require-File $file) }

$migration = Join-Path $backendSrc 'main/resources/db/migration/V4__profile_enrichment.sql'
if (Test-Path $migration) {
    $m = Get-Content $migration -Raw
    Add-Check 'Profile enrichment columns' ($m -match 'country' -and $m -match 'state_province' -and $m -match 'phone_number' -and $m -match 'photo_storage_key') 'country/state/phone/photo columns present'
    Add-Check 'Private photo key uniqueness' ($m -match 'UNIQUE \(photo_storage_key\)') 'photo storage key is unique'
    Add-Check 'Location index' ($m -match 'idx_user_profile_country_state_city') 'country/state/city index present'
    Add-Check 'Phone constraint' ($m -match 'ck_user_profile_phone_number') 'phone format constraint present'
}

$controller = Join-Path $backendSrc 'main/java/com/matrimony/profile/ProfileController.java'
if (Test-Path $controller) {
    $c = Get-Content $controller -Raw
    Add-Check 'Authenticated profile endpoints' ($c -match 'RequestMapping\("/api/v1/profile"\)' -and $c -match 'principal\(request\)') 'profile endpoints derive identity from auth principal'
    Add-Check 'Photo endpoints have no user id parameter' ($c -notmatch 'userId' -and $c -match 'photo') 'no client-supplied userId in profile photo controller'
}

$photoService = Join-Path $backendSrc 'main/java/com/matrimony/profile/ProfilePhotoService.java'
if (Test-Path $photoService) {
    $ps = Get-Content $photoService -Raw
    Add-Check 'Photo size limit' ($ps -match '5L \* 1024L \* 1024L') '5 MB server-side limit'
    Add-Check 'Image signature validation' ($ps -match '0xd8' -and $ps -match '0x89' -and $ps -match 'image/jpeg' -and $ps -match 'image/png') 'JPEG/PNG magic bytes and MIME type checked'
    Add-Check 'Generated storage key' ($ps -match 'UUID\.randomUUID\(\)') 'storage key is server generated'
    Add-Check 'Path traversal guard' ($ps -match 'normalize\(\)' -and $ps -match 'getParent\(\)') 'storage path is normalized and constrained'
}

$api = Join-Path $androidSrc 'main/java/com/matrimonyapp/data/remote/ProfileApi.kt'
if (Test-Path $api) {
    $a = Get-Content $api -Raw
    Add-Check 'Android profile PUT' ($a -match '@PUT\("api/v1/profile"\)') 'profile update endpoint present'
    Add-Check 'Android photo upload' ($a -match '@Multipart' -and $a -match '@POST\("api/v1/profile/photo"\)') 'multipart photo upload present'
    Add-Check 'Android photo delete' ($a -match '@DELETE\("api/v1/profile/photo"\)') 'photo delete endpoint present'
}

$sourceFiles = @()
if (Test-Path $androidSrc) { $sourceFiles += Get-ChildItem $androidSrc -Recurse -File | Where-Object { $_.Extension -in '.kt', '.xml' } }
if (Test-Path $backendSrc) { $sourceFiles += Get-ChildItem $backendSrc -Recurse -File | Where-Object { $_.Extension -in '.java', '.yml', '.sql' } }

foreach ($file in $sourceFiles) {
    $lines = Get-Content $file.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match 'r`n') { Add-Check 'Literal r`n scan' $false "$($file.FullName):$($i + 1)" }
        if ($lines[$i] -match '(?i)(trustAll|HostnameVerifier|X509TrustManager|sslContext)') { Add-Check 'Dangerous TLS scan' $false "$($file.FullName):$($i + 1)" }
        if ($lines[$i] -match '(?i)(password|token|authorization).*log(ger)?\s*\(') { Add-Check 'Credential logging scan' $false "$($file.FullName):$($i + 1)" }
    }
}

if ($script:checks.Count -eq 0) { Write-Host 'PHASE 08 VERIFIER: FAIL (no checks executed)'; exit 1 }
$failed = $script:checks | Where-Object { -not $_.Passed }
foreach ($check in $script:checks) {
    if ($check.Passed) { Write-Host "PASS: $($check.Name) - $($check.Detail)" }
    else { Write-Host "FAIL: $($check.Name) - $($check.Detail)" }
}
if ($failed.Count -gt 0) {
    Write-Host "PHASE 08 VERIFIER: FAIL ($($failed.Count) failed check(s))"
    exit 1
}
Write-Host 'PHASE 08 VERIFIER: PASS'
exit 0
