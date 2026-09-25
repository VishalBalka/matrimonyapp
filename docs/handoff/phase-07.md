# Phase 07 Handoff

## Scope

Phase 07 adds the authenticated user profile foundation:

- display name
- date of birth
- gender
- city
- short bio

The implementation preserves the Phase 06 authentication/session architecture and cloud/glass UI direction.

## Backend

Added:

- `ProfileModels`
- `ProfileValidator`
- `ProfileRepository`
- `ProfileService`
- `ProfileController`

Endpoints:

- `GET /api/v1/profile`
- `PUT /api/v1/profile`

Profile ownership is derived from the authenticated principal. The client does not provide an authoritative user ID.

## Database

Added Flyway migration:

`V3__profile_schema.sql`

The profile has a UUID primary key, a unique user relationship, foreign-key ownership, validation constraints, and timestamps.

## Android

Added:

- profile Retrofit API
- profile DTO/domain model
- profile repository
- profile validation
- profile screen

The Profile tab in the authenticated UI now opens the profile flow.

## Validation

Server-side validation is authoritative.

Rules include:

- display name required and max 100 characters
- date of birth must be ISO `YYYY-MM-DD`
- age must be 18 through 100
- gender must be MALE, FEMALE, or OTHER
- city required and max 120 characters
- bio max 500 characters

## Security

Profile endpoints require the existing authentication filter.

Profile ownership is derived from the server-side principal.

No passwords or session tokens are stored in profile data or logged.

Release cleartext behavior from Phase 06 is unchanged.

## Tests

Backend profile validator and service tests were added.

Android profile validation tests were added.

Required final commands:

```text
cd V:\Projects\MatrimonyApp\backend
..\android\gradlew.bat test

cd V:\Projects\MatrimonyApp\android
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug

cd V:\Projects\MatrimonyApp
.\scripts\verify-phase-07.ps1
```

## Known limitation

The profile screen uses the Material 3 date picker and sends the selected date as ISO `YYYY-MM-DD`.

Search and Matches remain placeholders and are not implemented by Phase 07.

## Git

Phase 06 checkpoint:

`3632a6f phase-06-integration`

Planned Phase 07 commit:

`phase-07-profile-foundation`
