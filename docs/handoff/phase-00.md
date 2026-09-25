# Phase 00 - Environment

## Status

PASSING WITH CONFIGURATION DECISIONS PENDING

## Verified

- JDK 17 executable works.
- Android SDK is installed.
- ADB is installed.
- Docker Desktop daemon is running.
- Git is installed.
- Git repository initialized on main branch.

## Environment Evidence

JDK 17:
Temurin 17.0.18

Docker:
Docker Engine 29.4.1
Docker Compose 5.1.3
Linux engine through Docker Desktop / WSL2

Git:
2.51.0.windows.1

ADB:
1.0.41
37.0.0-14910828

## Decisions To Confirm Before Backend

1. Neon PostgreSQL will be used unless local Docker PostgreSQL is deliberately retained.
2. Physical Android device will be the primary integration target.
3. Physical-device networking must replace the emulator-specific 10.0.2.2 assumption.

## Files Created

- CLAUDE.md
- docs/handoff/phase-00.md

## Next Phase

Phase 01 - Android foundation

## Important

Do not claim final application success from this handoff.

All future build, test, HTTP, APK, and runtime claims require actual command output.
