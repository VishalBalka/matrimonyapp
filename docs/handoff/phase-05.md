# Phase 05 - Authentication

## Status
Implementation patch prepared. Verification, backend tests, and real HTTP proof must be run against the user's current checkout before this phase is committed.

## Scope
- Email/password registration
- Email/password login
- Opaque server-side sessions
- Server-side logout/revocation
- Protected `/api/v1/auth/me`
- In-memory rate limiting by normalized email and source IP
- Generic login failure response
- Server-side validation
- BCrypt password hashing with explicit cost and maximum UTF-8 input size
- Configurable session TTL

## Endpoints
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`

## Password hashing
BCrypt cost 12. Maximum password length is 72 characters and passwords whose UTF-8 representation exceeds BCrypt's 72-byte input limit are rejected rather than truncated.

## Session design
A 256-bit cryptographically random opaque token is generated on successful login. Only its SHA-256 hash is persisted. The raw token is returned once in the login response. Sessions contain creation and expiration timestamps and can be revoked server-side.

## TTL
Normal local TTL: 15 minutes.
Test TTL: 60 seconds.
TTL is configuration-driven.

## Rate limiting
In-memory fixed-window buckets are separated by normalized email/account and source IP. Limits return HTTP 429 and do not permanently lock accounts.

## Verification commands
Run from `V:\Projects\matrimonyapp`:

```powershell
$env:POSTGRES_PASSWORD='your-local-password'
$env:DB_PASSWORD=$env:POSTGRES_PASSWORD
.\scripts\verify-phase-05.ps1
```

Run backend tests from `V:\Projects\matrimonyapp\backend` using the existing Android Gradle wrapper:

```powershell
..\android\gradlew.bat test
```

## Real HTTP proof required
Register, login, call the protected endpoint, logout, call the protected endpoint again, and verify the rejected session. Do not print the password, password hash, token, or Authorization header.

## Next phase
Phase 06 - Android/backend integration.
