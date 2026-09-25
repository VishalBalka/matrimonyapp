# Authentication & Authorization Architecture

## Authentication
- **Password Hashing:** Salted adaptive hashing (BCrypt with minimum work factor 12 or Argon2id).
- **Session Tokens:** Opaque cryptographically random tokens or short-lived signed JWTs.
- **Account Protection:** Account lock after 5 failed login attempts within 15 minutes.
- **Enumeration Resistance:** Generic responses returned for missing accounts on login and password reset.

## Authorization & RBAC
- **Roles:**
  - `MEMBER`: Regular registered matchmaking participant.
  - `ADMIN`: Platform moderator and operator.
- **Rule:** Client-submitted roles are never accepted. Roles are determined exclusively server-side from database records.
- **Ownership Verification:** A user can only read/edit their own profile (`/api/v1/profile`), while discoverable endpoints (`/api/v1/profiles/*`) redact sensitive fields according to recipient privacy toggles.
