# Phase 01 — Android Foundation

Status: **VERIFIED**
Verified on: 2026-09-14
Host: Windows 11, `V:\Projects\matrimonyApp`

## Scope

Minimal, buildable Kotlin + Jetpack Compose Android application under `android/`.

Deliberately excluded from this phase: authentication, OTP, networking of any
kind, dependency injection, annotation processing, launcher icon assets, and any
future-phase UI.

## Toolchain

| Item | Value | Where pinned |
| --- | --- | --- |
| AGP | 9.1.1 | `android/gradle/libs.versions.toml` |
| Gradle | 9.3.1 | `android/gradle/wrapper/gradle-wrapper.properties` |
| JDK | Eclipse Adoptium 17.0.18.8-hotspot | local `JAVA_HOME` |
| Kotlin | 2.4.20 | root `buildscript` classpath + version catalog |
| compileSdk / targetSdk | 37 / 37 | version catalog |
| minSdk | 26 | version catalog |
| Compose BOM | 2026.08.00 | version catalog |
| activity-compose | 1.13.0 | version catalog |
| Java / Kotlin JVM target | 17 | `android/app/build.gradle.kts` |
| SDK Build-Tools | 36.0.0 (AGP 9.1.1 default, auto-installed) | AGP default |

No dependency uses `latest` or a dynamic version.

## Kotlin plugin strategy

- `org.jetbrains.kotlin.android` / `kotlin-android` are **not** applied. AGP 9
  built-in Kotlin compiles the sources.
- `android.builtInKotlin` and `android.newDsl` are **not set**; AGP 9 defaults apply.
- No kapt, no KSP, no annotation processing anywhere in the build.
- AGP 9.1.1 declares KGP **2.2.10**. Kotlin 2.4.20 is obtained by adding
  `org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20` to the root buildscript
  classpath, which is the supported upgrade direction.
- `org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.20` is on the same
  classpath and `org.jetbrains.kotlin.plugin.compose` is applied in `:app`, so
  the Compose compiler matches the overridden Kotlin version instead of the one
  AGP would otherwise select.

### Kotlin version override — verified, not assumed

`gradlew buildEnvironment` (buildscript classpath):

```
+--- org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20
+--- org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.20
...
+--- org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10 -> 2.4.20
+--- org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.2.10 -> 2.4.20
```

The `2.2.10 -> 2.4.20` upgrade is the override taking effect over AGP's bundled KGP.

`gradlew :app:dependencies --configuration debugRuntimeClasspath`:

```
+--- org.jetbrains.kotlin:kotlin-stdlib:2.4.20
```

All transitive stdlib requests from AndroidX and Compose resolve up to 2.4.20.

Note: `kotlin-stdlib:{strictly 2.2.21}` appears on the **buildscript** classpath
only. That is Gradle 9.3.1's own embedded Kotlin and does not affect the app.

## Gradle wrapper

Generated locally with Gradle 9.3.1 in a scratch directory and copied in. Not
taken from any previous application.

| File | Size |
| --- | --- |
| `android/gradlew` | 8,618 bytes |
| `android/gradlew.bat` | 2,896 bytes |
| `android/gradle/wrapper/gradle-wrapper.jar` | 46,175 bytes |

## Verified results

### Build

```
gradlew :app:assembleDebug --console=plain
BUILD SUCCESSFUL in 8s
36 actionable tasks: 13 executed, 23 up-to-date
```

`:app:compileDebugKotlin` was in the executed set, not up-to-date.

### APK

```
V:\Projects\matrimonyApp\android\app\build\outputs\apk\debug\app-debug.apk
11,614,457 bytes — 2026-09-14 22:09:05
```

### Unit tests

```
gradlew :app:testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 7s
24 actionable tasks: 5 executed, 1 from cache, 18 up-to-date
```

`:app:compileDebugUnitTestKotlin` and `:app:testDebugUnitTest` both executed.

### Physical device

Debug APK installed and launched on a physical Android handset (2026-09-14
22:13). `MainActivity` rendered `FoundationScreen` with the dark colour scheme
applied, confirming `isSystemInDarkTheme()` and `MatrimonyAppTheme` resolve at
runtime. No emulator was used at any point in this phase.

## Known warnings (non-blocking)

1. `This version only understands SDK XML versions up to 3 but an SDK XML file
   of version 4 was encountered.` The installed `cmdline-tools` are older than
   the SDK packages they read. No build impact. Clears by updating
   `cmdline-tools;latest`.
2. `Unable to strip the following libraries, packaging them as they are:
   libandroidx.graphics.path.so`. AGP found no matching NDK strip tool, so the
   native library ships unstripped in the debug APK. No impact on this phase.

## Issues encountered and resolved

1. **Corrupt `settings.gradle.kts`.** A relative path passed to
   `[System.IO.File]::WriteAllText` resolved against .NET's working directory
   rather than the PowerShell location, fusing placeholder text onto the file
   head. Fixed by rewriting with absolute paths. Rule for later phases: never
   pass a relative path to a `System.IO.File` method from PowerShell.
2. **No system Gradle on PATH.** Wrapper bootstrapped from a downloaded Gradle
   9.3.1 distribution in a scratch directory, not inside the project.
3. **`AppSmokeTest.kt` in the wrong source set.** It was created under
   `app/src/main/java/...`, where JUnit is not on the classpath, causing
   `UNRESOLVED_IMPORT` during `:app:compileDebugKotlin`. Moved to
   `app/src/test/java/...`.

## Known cosmetic deviation

`app/src/main/res/values/strings.xml` on disk uses an em dash in
`phase_01_subheading` ("Phase 01 — Android foundation"). The bootstrap script
writes a plain hyphen. The committed file is the on-disk version. Cosmetic only.

## Not yet decided — carry into Phase 02

- Backend base URL strategy for a **physical device**. `10.0.2.2` is an emulator
  loopback alias and will not reach the development host from a handset. Options
  are LAN IP, `adb reverse`, or a tunnel. Not chosen yet.
- No `INTERNET` permission and no network security configuration exist yet. No
  cleartext HTTP or TLS bypass has been added for any build type, and none is to
  be added for release.
- Launcher icon and branding assets.
- Navigation, real screens, persistence.

Parent commit: 0a6df5f (phase-00-environment)
Tag: phase-01
