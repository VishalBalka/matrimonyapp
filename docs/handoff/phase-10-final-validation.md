# Phase 10 Final Validation

Date: 2026-09-18

## Scope

Final validation of the MatrimonyApp Android + Spring Boot project after profile enrichment.

## Environment

- Windows 11 development host
- JDK 17
- Gradle 9.3.1
- Android Gradle Plugin 9.1.1
- compileSdk 37
- targetSdk 37
- minSdk 26
- Kotlin 2.4.20
- Compose BOM 2026.08.00
- Spring Boot 4.1.1
- PostgreSQL 17.6
- Flyway migrations V1 through V4

## Git

Phase 09 checkpoint:

48a8be3 phase-09-profile

Phase 10 final commit is created after this handoff and final verification.

Temporary APPLY_PHASE_07_UI_POLISH.md and APPLY_PHASE_08.md files are not part of the application and must not be committed.

## Backend

Local PostgreSQL:

- Docker container: matrimonyapp-postgres
- image: postgres:17.6-alpine
- host port: 55432
- container port: 5432
- database: matrimony
- username: matrimony

Normal backend port:

8080

Normal configured session TTL:

15 minutes

Flyway runtime evidence showed PostgreSQL 17.6, four migrations validated, schema version 4, and the schema was up to date.

## Android

Debug API development hosts:

- emulator: 10.0.2.2
- physical device: <PC-LAN-IP>

Release cleartext networking remains fail-closed.

Debug APK:

V:\Projects\MatrimonyApp\android\app\build\outputs\apk\debug\app-debug.apk

APK size at validation:

19,726,860 bytes

## Static Verification

scripts\verify.ps1: PASS

Checks included required files, restricted debug HTTP hosts, 15-minute session TTL, exact V1 through V4 migration set, and required PostgreSQL password environment configuration.

scripts\verify-security.ps1: PASS

Checks included BCrypt password hashing, server-side token-hash persistence, Bearer-session authentication, logout revocation, Android Keystore, AES/GCM encryption, Bearer requests, 401 token clearing, and reduced-motion setting access.

scripts\verify-migrations.ps1: PASS

Checks included migration presence, duplicate-version detection, exact V1 through V4 sequence, Flyway configuration, profile enrichment fields, photo storage-key uniqueness, and location indexing.

scripts\verify-android.ps1: PASS

Checks included cleartext networking scope, release fail-closed behavior, reduced-motion handling, no Thread.sleep startup blocking, authentication entry points, forbidden auth mechanisms, profile fields, photo management, local backend targeting, Bearer authentication, secure token storage, and APK existence.

## Automated Tests

Backend:

..\android\gradlew.bat test

Result: BUILD SUCCESSFUL.

Android unit tests:

.\gradlew.bat :app:testDebugUnitTest

Result: BUILD SUCCESSFUL.

Android debug build:

.\gradlew.bat :app:assembleDebug

Result: BUILD SUCCESSFUL.

Build warnings were non-fatal and included an unnecessary safe call, an Elvis expression whose left operand is always used, an SDK XML warning, and a deprecated API warning in ProfilePhotoService.java.

## HTTP Runtime Authentication Evidence

A unique test account was registered against the normal backend on port 8080.

Registration:

HTTP 200

Login:

HTTP 200

Login token received:

true

Login expiry timestamp received:

true

Authenticated /me:

HTTP 200

Authenticated identity returned:

true

Logout:

HTTP 200

Reuse of the old session after logout:

HTTP 401

Result:

PASS

No password, token, Authorization header, or response body was included in the verification output.

## HTTP Session Expiration Evidence

A separate backend instance was started on port 8081 with a test-only session TTL of 1 second.

Registration:

HTTP 200

Login:

HTTP 200

Authenticated /me immediately after login:

HTTP 200

After a 3-second wait:

HTTP 401

Result:

PASS

This validates expiration behavior using a shortened test TTL. The normal application configuration remains 15 minutes.

## Database Migration Evidence

Flyway validated all four migrations:

- V1__initial_auth_schema.sql
- V2__auth_timestamps.sql
- V3__profile_schema.sql
- V4__profile_enrichment.sql

Runtime startup reported schema version 4 and no pending migration.

## Security Evidence

The validation set confirmed:

- BCrypt password hashing
- server-side session token hashing
- authenticated Bearer sessions
- logout revocation
- AndroidKeyStore token protection
- AES/GCM encryption
- Android 401 handling
- no arbitrary release cleartext traffic
- reduced-motion startup handling
- no startup Thread.sleep
- no active OTP authentication flow

## Profile Evidence

The Phase 09 implementation includes:

- country
- state/province
- city
- private phone
- profile photo management
- authenticated profile endpoints
- ownership based on authenticated principal
- photo storage protection
- profile enrichment migration V4

Physical-device profile/photo flow had previously been exercised successfully during implementation validation.

## Known Limitations

- Local development uses HTTP on explicitly approved debug hosts. Release networking remains fail-closed.
- Search and Matches remain authenticated-shell placeholders.
- Profile photo storage is local filesystem storage through the configured profile photo directory.
- No production deployment validation was performed.
- No production credentials were used.
- The final APK is a debug APK.
- The final report should not characterize the project as production-ready without separate production validation.

## Future OTP

OTP/SMS/social/MFA/password-reset functionality remains inactive. Future OTP work must be introduced explicitly in a later phase and must not be activated as part of the current authentication implementation.

## Closeout

Phase 10 runtime and static validation evidence is complete. The remaining closeout action is the final Git review and commit containing only Phase 10 documentation and verification scripts.

