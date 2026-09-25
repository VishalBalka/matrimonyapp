\# MatrimonyApp Project Instructions



\## Project

This repository contains the new MatrimonyApp Android + Spring Boot application.



Root:

V:\\Projects\\MatrimonyApp



Android:

V:\\Projects\\MatrimonyApp\\android



Backend:

V:\\Projects\\MatrimonyApp\\backend



\## Current Phase



Current phase: Phase 12 - Security, privacy, MFA, notifications and admin



Completed phases:

\- Phase 00 - Environment

\- Phase 01 - Android foundation

\- Phase 02 - Brand/startup

\- Phase 03 - Authentication UI

\- Phase 04 - Backend foundation

\- Phase 05 - Authentication backend

\- Phase 06 - Android/backend integration
- Phase 07 - Session security
- Phase 08 - Authenticated shell
- Phase 09 - Initial profile and profile enrichment
- Phase 10 - Final validation
- Phase 11 - Discovery/search
- Phase 12 - Security, privacy, MFA, notifications and admin

Next phase:

Phase 12 validation and closeout



\## Required Technology



Android:

\- JDK 17

\- Gradle 9.3.1

\- Android Gradle Plugin 9.1.1

\- compileSdk 37

\- targetSdk 37

\- minSdk 26

\- Kotlin 2.4.20

\- Compose BOM 2026.08.00

\- Java/Kotlin JVM target 17

\- AGP built-in Kotlin support



Backend:

\- Spring Boot 4.1.1

\- PostgreSQL 17.x

\- Flyway migrations



Do not use:

\- latest dependency versions

\- org.jetbrains.kotlin.android

\- kotlin-android

\- kapt

\- KSP

\- Hilt unless explicitly required by a later phase



\## Authentication



Only email/password authentication is active.



Active endpoints:

\- POST /api/v1/auth/register

\- POST /api/v1/auth/login

\- GET /api/v1/auth/me

\- POST /api/v1/auth/logout



Do not add:

\- OTP

\- SMS authentication

\- social login

\- MFA

\- password reset



Future OTP functionality must remain inactive until explicitly introduced by a later phase.



Authentication uses:

\- server-generated opaque session tokens

\- server-side token hashing

\- configurable session TTL

\- BCrypt password hashing

\- Android Keystore-backed encrypted token storage

\- Authorization: Bearer token



Normal session TTL:

15 minutes



\## Database



Local PostgreSQL runs through Docker.



Current local mapping:

\- host: localhost

\- host port: 55432

\- container port: 5432

\- database: matrimony

\- username: matrimony



The Windows host already owns port 5432, therefore Docker uses host port 55432.



Do not expose or commit real passwords.



Use environment variables for database passwords.



\## Android Networking



The Android debug build supports local development servers.



Supported debug hosts:

\- emulator: 10.0.2.2

\- physical Android device: 192.168.29.82



Release networking must remain fail-closed for cleartext HTTP.



Current physical-device development base URL:

http://192.168.29.82:8080/



Do not enable arbitrary cleartext traffic.



\## UI Direction



The visual direction is a premium cloud/glass aesthetic.



Use:

\- pastel blue

\- peach

\- cream

\- dark navy typography

\- cloud background

\- translucent/frosted glass surfaces

\- rounded glass controls

\- floating bottom navigation

\- Material icons



Avoid:

\- Unicode symbols used as UI icons

\- opaque rectangular controls where glass styling is intended

\- unnecessary visual clutter



\## Startup



Startup behavior must:

\- respect reduced-motion settings

\- read Settings.Global.ANIMATOR\_DURATION\_SCALE

\- skip animation when the scale is 0

\- avoid Thread.sleep

\- avoid blocking network operations

\- avoid network calls during startup animation



\## Source Editing Safety



Never perform destructive global regex replacement.



Before editing a source file:

1\. Inspect the affected file.

2\. Make the smallest necessary structural change.

3\. Re-read the affected file.

4\. Search for duplicate declarations.

5\. Check for accidental literal `r`n text.

6\. Compile/test the affected module.



Do not concatenate multiple PowerShell commands into one generated command when the commands modify source files.



Prefer complete file replacement when a file is short and the intended final content is known.



\## Verification



Phase verification scripts must:

\- be human-readable

\- report the file path

\- report the line number when possible

\- identify the violated rule

\- exit 0 when all checks pass

\- exit nonzero when checks fail



Phase 10 verification:

scripts\\verify-phase-06.ps1



Important checks include:

\- required Android files

\- Retrofit authentication endpoints

\- Android Keystore

\- AES/GCM token encryption

\- Authorization header

\- 401 handling

\- 429/409 handling

\- token clearing

\- authentication state

\- INTERNET permission

\- debug local hosts

\- release cleartext protection

\- literal `r`n

\- dangerous TLS configuration

\- credential/secret logging

\- active OTP UI

\- duplicate token-save behavior



\## Testing



Android unit tests:

cd V:\\Projects\\MatrimonyApp\\android

.\\gradlew.bat :app:testDebugUnitTest



Android debug APK:

.\\gradlew.bat :app:assembleDebug



Backend tests:

cd V:\\Projects\\MatrimonyApp\\backend

..\\android\\gradlew.bat test



Targeted authentication test:

..\\android\\gradlew.bat test --tests com.matrimony.auth.SessionServiceTest



\## Backend Local Run



From:

V:\\Projects\\MatrimonyApp\\backend



Use:



$env:POSTGRES\_PASSWORD="matrimony\_local\_only"

..\\android\\gradlew.bat bootRun --args="--spring.datasource.url=jdbc:postgresql://localhost:55432/matrimony --spring.datasource.username=matrimony --spring.datasource.password=matrimony\_local\_only"



Do not print credentials or authentication tokens in verification output.



\## Git



Git is mandatory.



Required phase commits:

\- phase-00-environment

\- phase-01-android-foundation

\- phase-02-brand-startup

\- phase-03-auth-ui

\- phase-04-backend

\- phase-05-authentication

\- phase-06-integration



Current expected HEAD after Phase 10 closeout:

phase-06-integration



Before committing:

1\. git status --short

2\. git diff --check

3\. run required verification

4\. run Android tests

5\. run Android debug build

6\. run relevant backend tests

7\. inspect staged diff



Do not commit generated build artifacts.



\## Failure Triage



For any distinct failure:

\- attempt repair at most twice

\- if two attempts fail, stop

\- create a blocker handoff

\- do not perform a speculative third repair



Do not claim a feature works without verification evidence.



\## Documentation



Phase handoffs belong under:



docs\\handoff\\



Required phase-06 handoff:



docs\\handoff\\phase-06.md



Handoffs should record:

\- scope

\- implementation

\- environment/toolchain versions

\- commands run

\- verification results

\- test results

\- known limitations

\- next phase

\- relevant local development decisions



\## Security Rules



Never:

\- hardcode production credentials

\- log passwords

\- log session tokens

\- disable TLS verification

\- trust all certificates

\- enable arbitrary cleartext traffic

\- add authentication bypasses

\- add hidden debug authentication

\- silently weaken security checks



\## Final Reporting



The final project report must use actual evidence.



Include:

\- environment/toolchain versions

\- final Git commit

\- project structure

\- PostgreSQL commands

\- backend commands

\- Android build commands

\- APK path

\- ZIP path if produced

\- test results

\- registration proof

\- login proof

\- protected /me proof

\- logout proof

\- expired-session proof

\- migration status

\- security verification

\- Android verification

\- limitations

\- future OTP locations



Never use unsupported claims such as:

\- "working"

\- "secure"

\- "production ready"



unless the corresponding verification evidence is documented.

