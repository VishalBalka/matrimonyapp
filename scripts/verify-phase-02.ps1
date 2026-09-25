$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$android = Join-Path $root "android"
$errors = @()

function Add-RuleError([string]$rule, [string]$path, [int]$line, [string]$message) {
    $script:errors += "[$rule] ${path}:$line - $message"
}

function Require-File([string]$path, [string]$rule) {
    if (-not (Test-Path $path -PathType Leaf)) {
        Add-RuleError $rule $path 1 "required file is missing"
        return $false
    }
    return $true
}

function Require-Text([string]$path, [string]$pattern, [string]$rule) {
    if (-not (Test-Path $path -PathType Leaf)) {
        Add-RuleError $rule $path 1 "file is missing"
        return
    }
    $lines = Get-Content $path
    $found = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -like "*$pattern*") { $found = $true; break }
    }
    if (-not $found) {
        Add-RuleError $rule $path 1 "required text '$pattern' was not found"
    }
}

$required = @(
    @{ Path = "app/src/main/res/drawable/brand_emblem.xml"; Rule = "brand emblem" },
    @{ Path = "app/src/main/res/drawable/ic_launcher_background.xml"; Rule = "launcher background" },
    @{ Path = "app/src/main/res/drawable/ic_launcher_monochrome.xml"; Rule = "launcher monochrome" },
    @{ Path = "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"; Rule = "adaptive launcher" },
    @{ Path = "app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml"; Rule = "adaptive round launcher" },
    @{ Path = "app/src/main/res/values/colors.xml"; Rule = "brand colors" },
    @{ Path = "app/src/main/java/com/matrimonyapp/ui/brand/BrandEmblem.kt"; Rule = "brand composable" },
    @{ Path = "app/src/main/java/com/matrimonyapp/ui/startup/StartupMotion.kt"; Rule = "startup motion" },
    @{ Path = "app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt"; Rule = "startup screen" },
    @{ Path = "app/src/main/java/com/matrimonyapp/ui/welcome/WelcomeScreen.kt"; Rule = "welcome screen" },
    @{ Path = "app/src/main/java/com/matrimonyapp/ui/AppScreen.kt"; Rule = "screen state" },
    @{ Path = "app/src/test/java/com/matrimonyapp/ui/startup/StartupMotionTest.kt"; Rule = "startup motion tests" }
)

foreach ($item in $required) {
    Require-File (Join-Path $android $item.Path) $item.Rule | Out-Null
}

$palette = @("#3A1424", "#8D3C58", "#C77B95", "#E8C98B", "#F6EBDD", "#CDBBC3")
$colorFile = Join-Path $android "app/src/main/res/values/colors.xml"
if (Test-Path $colorFile) {
    $colors = Get-Content $colorFile -Raw
    foreach ($hex in $palette) {
        if ($colors -notmatch [regex]::Escape($hex)) {
            Add-RuleError "brand palette" $colorFile 1 "missing $hex"
        }
    }
}

$manifest = Join-Path $android "app/src/main/AndroidManifest.xml"
Require-Text $manifest 'android:icon="@mipmap/ic_launcher"' "launcher icon declaration"
Require-Text $manifest 'android:roundIcon="@mipmap/ic_launcher_round"' "round launcher declaration"

$adaptive = Join-Path $android "app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml"
Require-Text $adaptive 'android:drawable="@drawable/brand_emblem"' "same emblem adaptive foreground"

$startup = Join-Path $android "app/src/main/java/com/matrimonyapp/ui/startup/StartupScreen.kt"
Require-Text $startup "Settings.Global.ANIMATOR_DURATION_SCALE" "reduced-motion system scale"
Require-Text $startup "onFinished()" "startup-to-welcome transition"
Require-Text $startup "goldSweep.animateTo" "visible gold sweep animation"
Require-Text $startup "stringResource(R.string.app_name)" "localized app name resource"

$sourceFiles = Get-ChildItem (Join-Path $android "app/src") -Recurse -File | Where-Object { $_.Extension -in @('.kt', '.kts', '.xml') }
foreach ($file in $sourceFiles) {
    $lines = Get-Content $file.FullName
    for ($i = 0; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -match 'Thread\.sleep') { Add-RuleError "no blocking UI sleep" $file.FullName ($i + 1) "Thread.sleep is forbidden in Phase 02" }
        # Check only executable/import/dependency declarations. Plain comments such as
        # "no Hilt, no KSP" are documentation, not active tooling.
        $code = $line -replace '//.*$', ''
        if ($code -match '^\s*import\s+.*\b(retrofit2|okhttp3|dagger|java\.net)\b') {
            Add-RuleError "Phase 02 dependency boundary" $file.FullName ($i + 1) "network/DI import is forbidden before later phases"
        }
        if ($file.Extension -eq '.kts' -and $code -match '(?i)(retrofit|okhttp|dagger|kapt|ksp)') {
            Add-RuleError "Phase 02 dependency boundary" $file.FullName ($i + 1) "network/DI build tooling or dependency is forbidden before later phases"
        }
        if ($line.Contains('`r`n')) { Add-RuleError "no literal r`n" $file.FullName ($i + 1) "literal PowerShell newline token found" }
    }
}

if ($errors.Count -gt 0) {
    Write-Host "PHASE 02 STATIC VERIFICATION: FAILED"
    $errors | ForEach-Object { Write-Host "ERROR: $_" }
    exit 1
}

Write-Host "PHASE 02 STATIC VERIFICATION: PASSED"
Write-Host "Checked required resources, launcher declarations, palette, reduced-motion handling, visible gold sweep, resource strings, and Phase 02 forbidden dependencies/calls."
exit 0
