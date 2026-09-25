# Phase 12 Security, Privacy, MFA, Notifications and Admin

## Scope

Phase 12 extends the Phase 11 discovery application with:

- TOTP-based multi-factor authentication.
- AES-GCM protected MFA secrets using `MFA_ENCRYPTION_KEY`.
- MFA login challenge and verification with five-attempt limit and expiry.
- Device credential app lock on Android.
- Profile visibility and profile-lock controls.
- Phone, salary and social-link visibility controls.
- Block and report workflows.
- Safety-awareness messaging.
- Profession, employer, salary range, education, skills and social/profile links.
- Background-check and verification status fields.
- Blue verification indicator when the server reports `VERIFIED`.
- Admin-only dashboard endpoints and Android dashboard.
- Admin verification, background-check, report-status and profile-lock controls.
- PostgreSQL notification persistence and authenticated SSE notification stream.
- Development dummy profiles in migration V5 for local discovery testing.
- Higher-contrast CloudNavySoft text and readable date-picker disabled text.

## MFA

MFA is TOTP-based rather than SMS/email OTP. The user enters the generated secret into an authenticator application and confirms a six-digit time-based code.

The server never logs the MFA secret or code. MFA secrets are encrypted with AES-GCM. Production deployments must provide a 32-byte Base64 `MFA_ENCRYPTION_KEY`.

## Admin

The normal authentication response contains the server-side role. Admin dashboard endpoints require `ADMIN`. Optional bootstrap credentials are supplied through `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and `ADMIN_DISPLAY_NAME`.

Do not place administrator credentials in source control.

## Notifications

Notifications are persisted in PostgreSQL and delivered over an authenticated Server-Sent Events stream while the Android authenticated session is active.

## Dummy data

V5 inserts six `example.test` demo accounts and profiles. They are development fixtures, not verified real people. They contain no passwords and cannot be used as normal accounts.

## Validation status

The Phase 12 source package was generated from the user's Phase 11 source ZIP. Android compilation could not be executed in the build environment because the Gradle distribution was not available offline. Windows validation must run the Android unit tests/build and backend tests before treating the package as build-verified.

Production release still requires environment-provided database credentials, MFA encryption key, admin bootstrap credentials, HTTPS, and an operational notification/push strategy if background notifications are required outside an active SSE connection.
