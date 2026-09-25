# Phase 02 — Brand + Startup

Status: PENDING LOCAL VERIFICATION

## Scope

Brand palette, shared emblem, adaptive launcher icon, startup animation, reduced-motion handling, and welcome screen.

## Decisions

- D1: Plain `AppScreen` state hoisting with `rememberSaveable`.
- D2: Keep both light and dark Material 3 schemes. Startup uses the dark brand window/theme presentation.
- D3: Declare Compose Foundation and Animation explicitly, with versions supplied by the Compose BOM.
- D4: Remove obsolete Phase 01 heading/subheading strings.
- D5: One `brand_emblem.xml` is reused for launcher foreground, startup, and welcome.
- D6: Build Tools remains unpinned.

## Verification

Run locally and paste the actual outputs below. Do not replace these placeholders with expected results.

### Static verification

```text
[ paste scripts/verify-phase-02.ps1 output here ]
```

### Unit tests

```text
[ paste .\gradlew.bat :app:testDebugUnitTest --console=plain output here ]
```

### Build

```text
[ paste .\gradlew.bat clean :app:assembleDebug --console=plain --stacktrace output here ]
```

### Device verification

```text
[ paste Test-Path, adb devices -l, and adb install output here ]
```

### Git

Commit only after the local verification passes:

```text
[ paste git status --short and git diff --stat here ]
```
