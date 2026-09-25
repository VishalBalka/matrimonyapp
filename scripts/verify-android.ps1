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

function Check-File($relativePath) {
    $path = Join-Path $root $relativePath
    if (Test-Path $path -PathType Leaf) {
        Pass "Required Android file - $relativePath"
        return $true
    }

    Fail $relativePath $null "required-file" "File does not exist."
    return $false
}

function Check-Pattern($relativePath, $pattern, $rule, $message) {
    $path = Join-Path $root $relativePath

    if (-not (Test-Path $path -PathType Leaf)) {
        Fail $relativePath $null "required-file" "File does not exist."
        return
    }

    $lines = Get-Content $path

    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            Pass "${relativePath}:$($i + 1) - $message"
            return
        }
    }

    Fail $relativePath $null $rule $message
}

Write-Host "PHASE 12 ANDROID VERIFICATION"

$required = @(
    "android/app/src/main/AndroidManifest.xml",
    "android/app/src/main/java/com/matrimonyapp/MainActivity.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupMotion.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/welcome/WelcomeScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/auth/LoginScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/auth/RegistrationScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/auth/AuthenticatedScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt",
    "android/app/src/main/java/com/matrimonyapp/data/remote/AuthApi.kt",
    "android/app/src/main/java/com/matrimonyapp/data/remote/ProfileApi.kt",
    "android/app/src/main/java/com/matrimonyapp/data/repository/AuthRepository.kt",
    "android/app/src/main/java/com/matrimonyapp/data/profile/ProfileRepository.kt",
    "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt"
)

foreach ($file in $required) {
    [void](Check-File $file)
}

$manifest = Join-Path $root "android/app/src/main/AndroidManifest.xml"
if (Test-Path $manifest) {
    $manifestContent = Get-Content $manifest -Raw

    if ($manifestContent -match 'android:usesCleartextTraffic="false"') {
        Pass "Release manifest does not globally permit cleartext traffic"
    } elseif ($manifestContent -match 'android:usesCleartextTraffic="true"') {
        Fail "android/app/src/main/AndroidManifest.xml" $null `
            "cleartext-release" "Manifest globally enables cleartext traffic."
    } else {
        Pass "Manifest does not globally enable cleartext traffic"
    }
}

$networkConfig = Join-Path $root "android/app/src/debug/res/xml/network_security_config.xml"
if (Test-Path $networkConfig) {
    $networkContent = Get-Content $networkConfig -Raw

    if ($networkContent -match '10\.0\.2\.2' -and $networkContent -match '192\.168\.29\.82') {
        Pass "Debug network security permits approved local development hosts"
    } else {
        Fail "android/app/src/debug/res/xml/network_security_config.xml" $null `
            "debug-network-hosts" "Approved emulator and physical-device development hosts were not both found."
    }

    if ($networkContent -match 'cleartextTrafficPermitted="true"') {
        Pass "Debug cleartext permission is explicitly scoped to development configuration"
    } else {
        Fail "android/app/src/debug/res/xml/network_security_config.xml" $null `
            "debug-cleartext" "Expected explicit debug cleartext permission was not found."
    }
} else {
    Fail "android/app/src/debug/res/xml/network_security_config.xml" $null `
        "required-file" "Debug network security configuration is missing."
}

$mainManifest = Join-Path $root "android/app/src/main/AndroidManifest.xml"
$debugManifest = Join-Path $root "android/app/src/debug/AndroidManifest.xml"

$mainManifestContent = if (Test-Path $mainManifest) {
    Get-Content $mainManifest -Raw
} else {
    ""
}

$debugManifestContent = if (Test-Path $debugManifest) {
    Get-Content $debugManifest -Raw
} else {
    ""
}

if ($mainManifestContent -notmatch 'usesCleartextTraffic="true"' -and
    $mainManifestContent -notmatch 'networkSecurityConfig=') {
    Pass "Release has no global cleartext permission or debug network-security override"
} else {
    Fail "android/app/src/main/AndroidManifest.xml" $null `
        "release-cleartext" "Release manifest contains a global cleartext permission or network-security override."
}

if ($debugManifestContent -match 'networkSecurityConfig="@xml/network_security_config"') {
    Pass "Network security configuration is scoped to debug manifest"
} else {
    Fail "android/app/src/debug/AndroidManifest.xml" $null `
        "debug-network-config" "Debug network security configuration is not scoped through the debug manifest."
}

$startupScreen = Join-Path $root "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt"
if (Test-Path $startupScreen) {
    $startupContent = Get-Content $startupScreen -Raw

    if ($startupContent -match 'Settings\.Global\.ANIMATOR_DURATION_SCALE') {
        Pass "Startup reads Android animator duration scale"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt" $null `
            "reduced-motion" "Android animator duration scale read is missing."
    }

    if ($startupContent -match 'startupDurationMillis\(animatorScale\)\s*==\s*0L') {
        Pass "Startup skips animation when reduced-motion scale is zero"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt" $null `
            "reduced-motion-zero" "Zero animator scale does not visibly skip the startup animation."
    }

    if ($startupContent -notmatch 'Thread\.sleep') {
        Pass "Startup contains no Thread.sleep blocking call"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt" $null `
            "startup-blocking" "Thread.sleep is forbidden in startup code."
    }
}

$welcome = Join-Path $root "android/app/src/main/java/com/matrimonyapp/ui/welcome/WelcomeScreen.kt"
if (Test-Path $welcome) {
    $welcomeContent = Get-Content $welcome -Raw

    if ($welcomeContent -match 'Sign In' -or $welcomeContent -match 'signIn' -or $welcomeContent -match 'Create Account' -or $welcomeContent -match 'createAccount') {
        Pass "Welcome screen contains authentication entry points"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/ui/welcome/WelcomeScreen.kt" $null `
            "welcome-actions" "Sign In/Create Account entry points were not detected."
    }
}

$authFiles = @(
    "android/app/src/main/java/com/matrimonyapp/ui/auth/LoginScreen.kt",
    "android/app/src/main/java/com/matrimonyapp/ui/auth/RegistrationScreen.kt"
)

foreach ($file in $authFiles) {
    $path = Join-Path $root $file
    if (Test-Path $path) {
        $content = Get-Content $path -Raw

        if ($content -match '(?i)\b(otp|one[- ]time password|social login|google sign|facebook sign)\b') {
            Fail $file $null "forbidden-auth-ui" "Forbidden authentication mechanism text or UI was detected."
        } else {
            Pass "$file contains no forbidden authentication mechanism"
        }
    }
}

$profile = Join-Path $root "android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt"
if (Test-Path $profile) {
    $profileContent = Get-Content $profile -Raw

    foreach ($field in @("country", "state", "city", "phone")) {
        if ($profileContent -match "(?i)\b$field\b") {
            Pass "Profile UI contains $field field"
        } else {
            Fail "android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt" $null `
                "profile-$field" "Profile UI field '$field' was not detected."
        }
    }

    if ($profileContent -match '(?i)(upload|change|remove).{0,80}(photo|image)|photo.{0,80}(upload|change|remove)') {
        Pass "Profile UI contains photo management actions"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/ui/profile/ProfileScreen.kt" $null `
            "profile-photo-ui" "Photo management actions were not detected."
    }
}

$apiClient = Join-Path $root "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt"
if (Test-Path $apiClient) {
    $apiContent = Get-Content $apiClient -Raw

    if ($apiContent -match '192\.168\.29\.82' -and $apiContent -match '8080') {
        Pass "Android debug API client targets configured local backend host"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt" $null `
            "api-host" "Configured local backend host was not detected."
    }

    if ($apiContent -match 'Authorization' -and $apiContent -match 'Bearer') {
        Pass "Android API client sends authenticated Bearer requests"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/data/remote/ApiClient.kt" $null `
            "api-auth" "Bearer Authorization handling was not detected."
    }
}

$tokenStore = Join-Path $root "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt"
if (Test-Path $tokenStore) {
    $tokenContent = Get-Content $tokenStore -Raw

    if ($tokenContent -match 'AndroidKeyStore' -and $tokenContent -match 'AES/GCM/NoPadding') {
        Pass "Secure token store uses AndroidKeyStore with AES/GCM"
    } else {
        Fail "android/app/src/main/java/com/matrimonyapp/core/security/SecureTokenStore.kt" $null `
            "token-storage" "Expected AndroidKeyStore AES/GCM protection was not detected."
    }
}

$apk = Join-Path $root "android/app/build/outputs/apk/debug/app-debug.apk"
if (Test-Path $apk -PathType Leaf) {
    $size = (Get-Item $apk).Length
    if ($size -gt 0) {
        Pass "Debug APK exists and is non-empty"
    } else {
        Fail "android/app/build/outputs/apk/debug/app-debug.apk" $null `
            "apk-output" "Debug APK exists but is empty."
    }
} else {
    Fail "android/app/build/outputs/apk/debug/app-debug.apk" $null `
        "apk-output" "Debug APK was not found. Run assembleDebug first."
}

if ($failures -eq 0) {
    Write-Host "PHASE 12 ANDROID VERIFICATION: PASS"
    exit 0
}

Write-Host "PHASE 12 ANDROID VERIFICATION: FAIL ($failures failure(s))"
exit 1