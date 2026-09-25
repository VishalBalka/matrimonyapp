# Phase 08 Handoff: Profile Enrichment and Private Contact

## Scope

Phase 08 extends the authenticated profile foundation with a profile photo, country, state/province, and optional private phone number. Search and Matches remain placeholders.

## Backend

- Flyway `V4__profile_enrichment.sql` adds country, state/province, phone number, and photo storage metadata.
- `PUT /api/v1/profile` remains authenticated and derives ownership from the authenticated principal.
- `GET /api/v1/profile` returns the signed-in user's own enriched profile.
- `POST /api/v1/profile/photo` uploads a JPEG/PNG photo.
- `GET /api/v1/profile/photo` returns the signed-in user's own photo.
- `DELETE /api/v1/profile/photo` removes the signed-in user's photo.
- No profile photo or contact endpoint accepts a client-supplied user ID.
- Photos are stored under a configurable server directory using UUID-generated filenames.
- Server photo limit is 5 MB. JPEG/PNG magic bytes and declared MIME type are checked.
- Phone is optional and is not part of any public discovery endpoint in this phase.

## Android

- Retrofit supports enriched profile data and photo upload/download/delete.
- Profile editor includes country, state/province, city, and optional private phone.
- Profile details include a circular photo with add/change/remove controls.
- Photo selection uses the Android system document picker, so broad storage permissions are not required.
- Client validation mirrors the backend limits for location and phone fields.

## Verification

Run from `V:\Projects\MatrimonyApp\backend` with the existing local PostgreSQL environment variables set for port `55432`:

```powershell
$env:POSTGRES_PASSWORD="<DB_PASSWORD>"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:55432/matrimony"
$env:SPRING_DATASOURCE_USERNAME="matrimony"
$env:SPRING_DATASOURCE_PASSWORD="<DB_PASSWORD>"
..\android\gradlew.bat test
```

Run Android tests and build from `V:\Projects\MatrimonyApp\android`:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Run the static verifier from `V:\Projects\MatrimonyApp`:

```powershell
.\scripts\verify-phase-08.ps1
```

Do not commit until all three test/build/verifier checks pass and the physical phone verifies profile editing and photo flows.

