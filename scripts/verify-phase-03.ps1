$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$errors = @()
function Check([bool]$ok, [string]$path, [int]$line, [string]$rule) { if (-not $ok) { $script:errors += "ERROR: ${path}:$line - $rule" } }
$main = Join-Path $root 'android/app/src/main/java/com/matrimonyapp/MainActivity.kt'
$auth = Join-Path $root 'android/app/src/main/java/com/matrimonyapp/ui/auth'
Check (Test-Path $main) $main 1 'MainActivity is required'
Check (Test-Path (Join-Path $auth 'LoginScreen.kt')) (Join-Path $auth 'LoginScreen.kt') 1 'Login UI is required'
Check (Test-Path (Join-Path $auth 'RegistrationScreen.kt')) (Join-Path $auth 'RegistrationScreen.kt') 1 'Registration UI is required'
$files = Get-ChildItem (Join-Path $root 'android/app/src/main') -Recurse -File -Include *.kt,*.kts
foreach ($f in $files) { $n=0; foreach ($line in Get-Content $f.FullName) { $n++; $code=$line -replace '//.*$',''; Check (-not ($code -match '(?i)otp|google login|apple login|mfa|2fa|password reset')) $f.FullName $n 'forbidden authentication feature token before its phase' ; Check (-not ($line -match 'r`n')) $f.FullName $n 'literal r`n is forbidden' } }
$mainText = Get-Content $main -Raw
Check ($mainText -match 'screenKey = "login"') $main 1 'Welcome must navigate to Login'
Check ($mainText -match 'screenKey = "registration"') $main 1 'Welcome/Login must navigate to Registration'
$validation = Join-Path $auth 'AuthValidation.kt'
$vt = Get-Content $validation -Raw
Check ($vt -match 'MIN_PASSWORD_LENGTH = 10') $validation 1 'registration password minimum must be 10'
Check ($vt -match 'MAX_PASSWORD_LENGTH = 128') $validation 1 'registration password maximum must be explicit'
Check ($vt -match 'password != confirmPassword') $validation 1 'confirm password must require exact equality'
if ($errors.Count -gt 0) { Write-Host 'PHASE 03 STATIC VERIFICATION: FAILED'; $errors | ForEach-Object { Write-Host $_ }; exit 1 }
Write-Host 'PHASE 03 STATIC VERIFICATION: PASSED'
Write-Host 'Checked Login, Registration, validation, navigation, and forbidden authentication features.'
exit 0
