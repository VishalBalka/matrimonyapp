# MatrimonyApp Phase 12 package

This package is based on the supplied Phase 11 source tree.

Implemented in this package:
- TOTP MFA with protected server-side secret storage.
- Device-credential app lock.
- Profile privacy, visibility and profile lock.
- Block and report.
- Safety-awareness guidance.
- Profession, employer, salary range, education, skills and HTTPS social links.
- Admin verification and blue verification status.
- Background-check status.
- Admin dashboard and audit log.
- PostgreSQL notifications plus authenticated SSE delivery.
- Development dummy profiles in Flyway V5.
- Higher-contrast UI and date-picker text.

Important:
- This package is source code. It is not represented as a successfully compiled Phase 12 APK.
- Android compilation could not be performed in the offline build environment because Gradle 9.3.1 was not available locally.
- Run backend tests, Android unit tests, Android debug assembly and runtime HTTP tests on Windows before release.
- Provide `MFA_ENCRYPTION_KEY`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and database credentials through the environment.
- The admin bootstrap password must never be committed.
- V5 contains `example.test` dummy profiles for local testing only.
- TOTP MFA is authenticator-app based. SMS/email OTP delivery is not included because that requires a real delivery provider.
