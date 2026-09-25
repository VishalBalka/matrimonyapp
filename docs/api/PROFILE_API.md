# Profile & Search & Identity APIs

## Identity (`/api/v1/auth/me`)
- **GET** `/api/v1/auth/me`: Resolves current authenticated session principal, display name, email, role, and MFA state.

## Profile Management (`/api/v1/profile`)
- **GET** `/api/v1/profile`: Retrieve personal profile details, background check status, verified date, and contact privacy settings.
- **PUT** `/api/v1/profile`: Atomically update profile fields:
  - Required: `displayName`, `dateOfBirth` (YYYY-MM-DD), `gender`, `country`, `stateProvince`, `city`.
  - Optional: `bio` (up to 500 chars), `phoneNumber`, `profession`, `employer`, `salaryRange`, `education`, `skills`, `linkedinUrl`, `instagramUrl`, `facebookUrl`, `websiteUrl`.
  - Visibility Toggles: `profileVisibility` ("PUBLIC", "PRIVATE", "LOCKED"), `showPhone`, `showSalary`, `showSocial`, `profileLocked`.
- **POST** `/api/v1/profile/photo`: Multipart form-data image upload (JPEG/PNG up to 5 MB).
- **DELETE** `/api/v1/profile/photo`: Remove profile photo.

## Discovery Search (`/api/v1/profiles/search`)
- **GET** `/api/v1/profiles/search`:
  - Query parameters: `minAge`, `maxAge`, `city`, `state`, `country`, `page`, `pageSize`.
  - Returns paginated list of verified discovery profiles.
  - Safe sanitization: Does not leak phone numbers, full birthdates, or private contact details.

## Profile Detail (`/api/v1/profiles/{id}`)
- **GET** `/api/v1/profiles/{id}`:
  - Retrieves full verified details for discoverable profile ID.
  - Respects recipient's visibility toggles for salary and social URLs.
