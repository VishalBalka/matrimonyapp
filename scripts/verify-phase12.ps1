$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$failures = 0

function Pass($message) { Write-Host "PASS: $message" }
function Fail($path,$line,$rule,$message) {
    $script:failures++
    if($line){Write-Host "FAIL: ${path}:$line - $rule - $message"}else{Write-Host "FAIL: $path - $rule - $message"}
}

$required = @(
    "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql",
    "backend/src/main/java/com/matrimony/auth/MfaService.java",
    "backend/src/main/java/com/matrimony/auth/MfaCrypto.java",
    "backend/src/main/java/com/matrimony/auth/Totp.java",
    "backend/src/main/java/com/matrimony/security/SecurityController.java",
    "backend/src/main/java/com/matrimony/security/SecurityService.java",
    "backend/src/main/java/com/matrimony/notification/NotificationController.java",
    "backend/src/main/java/com/matrimony/notification/NotificationService.java",
    "backend/src/main/java/com/matrimony/admin/AdminController.java",
    "backend/src/main/java/com/matrimony/admin/AdminService.java",
    "android/app/src/main/java/com/matrimonyapp/ui/security/SecurityScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/security/AppLockScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/notification/NotificationScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/admin/AdminScreen.kt"
)

foreach($relative in $required){
    if(Test-Path (Join-Path $root $relative)){Pass "Required Phase 12 file - $relative"}
    else{Fail $relative $null "required-file" "Phase 12 file is missing."}
}

$v5=Join-Path $root "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql"
if(Test-Path $v5){
    $content=Get-Content $v5 -Raw
    foreach($pattern in @("mfa_factor","mfa_challenge","user_block","user_report","app_notification","admin_audit_log","verification_status","background_check_status","linkedin_url","salary_range","profile_visibility")){
        if($content -match [regex]::Escape($pattern)){Pass "V5 includes $pattern"}else{Fail "backend/src/main/resources/db/migration/V5__security_privacy_admin.sql" $null "v5-schema" "V5 is missing $pattern."}
    }
}

$mfa=Join-Path $root "backend/src/main/java/com/matrimony/auth/MfaService.java"
if(Test-Path $mfa){
    $content=Get-Content $mfa -Raw
    if($content -match "attempts\s*>=\s*5"){Pass "MFA has an attempt limit"}else{Fail "backend/src/main/java/com/matrimony/auth/MfaService.java" $null "mfa-attempts" "MFA attempt limit was not found."}
    if($content -match "plus\(Duration\.ofMinutes\(5\)\)"){Pass "MFA challenge has expiry"}else{Fail "backend/src/main/java/com/matrimony/auth/MfaService.java" $null "mfa-expiry" "MFA challenge expiry was not found."}
}

$admin=Join-Path $root "backend/src/main/java/com/matrimony/admin/AdminController.java"
if((Test-Path $admin) -and (Get-Content $admin -Raw) -match "isAdmin"){Pass "Admin endpoints require ADMIN role"}else{Fail "backend/src/main/java/com/matrimony/admin/AdminController.java" $null "admin-authz" "Admin role authorization was not found."}

$security=Join-Path $root "backend/src/main/java/com/matrimony/security/SecurityController.java"
if((Test-Path $security) -and (Get-Content $security -Raw) -match "/mfa/setup"){Pass "Security controller exposes MFA setup"}else{Fail "backend/src/main/java/com/matrimony/security/SecurityController.java" $null "mfa-endpoint" "MFA setup endpoint was not found."}

$notifications=Join-Path $root "backend/src/main/java/com/matrimony/notification/NotificationController.java"
if((Test-Path $notifications) -and (Get-Content $notifications -Raw) -match "TEXT_EVENT_STREAM"){Pass "Authenticated SSE notification stream exists"}else{Fail "backend/src/main/java/com/matrimony/notification/NotificationController.java" $null "notification-stream" "SSE notification stream was not found."}

$profile=Join-Path $root "backend/src/main/java/com/matrimony/profile/ProfileValidator.java"
if((Test-Path $profile) -and (Get-Content $profile -Raw) -match "https"){Pass "Profile links require HTTPS"}else{Fail "backend/src/main/java/com/matrimony/profile/ProfileValidator.java" $null "link-validation" "HTTPS profile-link validation was not found."}

Write-Host "PHASE 12 STATIC FEATURE VERIFICATION"
if($failures -gt 0){exit 1}
Write-Host "PHASE 12 STATIC FEATURE VERIFICATION: PASS"
exit 0
