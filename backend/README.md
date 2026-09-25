# MatrimonyApp Backend

Production-ready, highly secure Spring Boot 3 / Java 17+ backend service for the **MatrimonyApp** mobile and web platform.

---

## 1. Architecture Overview

The backend is built following a clean, layered architectural pattern:

```
com.matrimonyapp.backend/
├── config/             # Security filters, OpenAPI, Rate-limiting, Application properties
├── controller/         # REST Controllers (Auth, Profile, Discovery, Security, Admin, Health)
├── dto/                # Request & Response Data Transfer Objects with validation annotations
├── entity/             # JPA Entities (User, Profile, Photo, Session, OTP, MFA, Blocks, Reports, Audit)
├── exception/          # Custom exceptions, Error codes, and RFC-7807 compliant GlobalExceptionHandler
├── repository/         # Spring Data JPA Repositories with custom secure queries
├── security/           # JWT Provider, UserPrincipal, CustomUserDetailsService, TOTP Service
└── service/            # Core business logic (Auth, Profile, Photo, Discovery, Security, Admin, Audit)
```

### Key Technical Stack
- **Framework:** Spring Boot 3.3.4
- **Language:** Java 17 / 21
- **Security:** Spring Security 6, JJWT 0.12.6, Argon2id & BCrypt password hashing, TOTP MFA
- **Database:** PostgreSQL 16 (H2 for integration tests)
- **Migrations:** Flyway
- **Validation:** Jakarta Bean Validation (Hibernate Validator)
- **Documentation:** SpringDoc OpenAPI 2.6.0 (Swagger UI)
- **Monitoring:** Spring Boot Actuator

---

## 2. Security Model & Authentication Flows

### Threat Protection & Hardening
- **Password Hashing:** BCrypt with high work factor (salt rounds) & Argon2 support via BouncyCastle.
- **Access Tokens:** Short-lived JWTs (24h default, configurable) signed with HMAC-SHA256.
- **Refresh & Session Revocation:** Server-side `sessions` table tracks active tokens. Logout instantly revokes the session hash in the database, preventing token reuse.
- **Account Lockout:** Automatic account lockout after 5 consecutive failed login attempts with progressive cooldown.
- **Rate Limiting:** Sliding-window rate limiter per client IP protecting `/api/v1/auth/login` (10 req/5 min), `/api/v1/auth/register` (5 req/min), `/api/v1/auth/otp/*` (3 req/10 min). Returns HTTP 429 (`TOO_MANY_REQUESTS`).
- **Authorization & Data Isolation:** All profile lookups derive the caller from the authenticated JWT `UserPrincipal`. User A cannot alter User B's profile or view hidden attributes (salary, phone number) unless privacy rules allow.

---

## 3. OTP & Multi-Factor Authentication (MFA) Workflows

### OTP Security
- **Purposes:** `REGISTRATION`, `LOGIN`, `PASSWORD_RESET`, `PHONE_VERIFICATION`, `EMAIL_VERIFICATION`, `MFA`, `SECURITY_ACTION`.
- **Hash-only Storage:** Raw OTP values are never persisted in the database; only cryptographic SHA-256 hashes are stored.
- **Lifecycle:** 5-minute expiration, max 3 attempts before permanent invalidation, single-use consumption.
- **Account Enumeration Defense:** Generic HTTP 200 responses returned regardless of whether the target email exists.

### TOTP MFA Flow
1. **Enrollment (`/api/v1/security/mfa/setup`):** Generates a high-entropy Base32 secret and QR code URI (`otpauth://totp/...`) compatible with Google Authenticator. Ten 8-character single-use recovery codes are generated and stored as SHA-256 hashes.
2. **Confirmation (`/api/v1/security/mfa/verify`):** Requires the client to present a valid TOTP code before activating `mfa_enabled = true`.
3. **Login Challenge:** When MFA is active, login returns `mfaRequired: true` and an ephemeral single-use `mfaSessionId`. The client must exchange this at `/api/v1/auth/mfa/verify` with a TOTP or recovery code to receive the final JWT access token.

---

## 4. Database Setup & Docker Compose

### Prerequisites
- Docker & Docker Compose
- JDK 17 or higher

### Local PostgreSQL 16 Setup
Start the local PostgreSQL container using the included `docker-compose.yml`:

```bash
cd backend
docker compose up -d
```

Database connection settings (pre-configured in `application-dev.yml`):
- **Host:** `localhost`
- **Port:** `55432`
- **Database:** `matrimony`
- **Username:** `matrimony`
- **Password:** `matrimony_local_only`

To stop the database:
```bash
docker compose down
```

---

## 5. Building & Running the Application

### Running Locally (Development Mode)
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Running Automated Tests
```bash
cd backend
./gradlew test
```
All tests run against an in-memory PostgreSQL-mode H2 database with Flyway schema migration validation.

---

## 6. API Reference & Swagger UI

Once the application is running, access interactive OpenAPI documentation:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI v3 JSON:** `http://localhost:8080/v3/api-docs`
- **Actuator Health:** `http://localhost:8080/actuator/health`

### Main Endpoints Summary

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET`  | `/api/health` | Service and database health check | No |
| `POST` | `/api/v1/auth/register` | Register new account | No |
| `POST` | `/api/v1/auth/login` | Authenticate with email/password | No |
| `POST` | `/api/v1/auth/mfa/verify` | Verify MFA challenge during login | No |
| `POST` | `/api/v1/auth/logout` | Revoke session and invalidate token | Yes |
| `GET`  | `/api/v1/auth/me` | Fetch authenticated identity | Yes |
| `POST` | `/api/v1/auth/otp/request` | Request an OTP for a bound purpose | No |
| `POST` | `/api/v1/auth/otp/verify` | Validate OTP for bound purpose | No |
| `GET`  | `/api/v1/profile` | Get current user's profile | Yes |
| `PUT`  | `/api/v1/profile` | Update profile fields & privacy | Yes |
| `POST` | `/api/v1/profile/photo` | Upload user profile picture | Yes |
| `GET`  | `/api/v1/discovery/search` | Search matrimonial profiles (paginated) | Yes |
| `GET`  | `/api/v1/discovery/profiles/{id}`| View another user's profile | Yes |
| `POST` | `/api/v1/security/block/{userId}` | Block user | Yes |
| `DELETE`| `/api/v1/security/block/{userId}` | Unblock user | Yes |
| `POST` | `/api/v1/security/report` | Report profile/user | Yes |
| `GET`  | `/api/v1/security/mfa/setup` | Initiate TOTP MFA setup | Yes |
| `POST` | `/api/v1/security/mfa/verify`| Activate MFA with code | Yes |
| `POST` | `/api/v1/security/mfa/disable` | Disable MFA | Yes |
| `GET`  | `/api/v1/notifications` | List user notifications | Yes |
| `GET`  | `/api/v1/admin/dashboard` | Platform statistics (Admin only) | Admin |
| `GET`  | `/api/v1/admin/reports` | Moderation queue (Admin only) | Admin |
| `POST` | `/api/v1/admin/verify` | Verify member profile (Admin only) | Admin |
| `POST` | `/api/v1/admin/status` | Suspend/Activate account (Admin only) | Admin |

---

## 7. Sample cURL Requests

### 1. Register a New Account
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "displayName": "Ananya Sharma",
    "email": "ananya.sharma@example.com",
    "password": "SecurePassword123!",
    "confirmPassword": "SecurePassword123!"
  }'
```

### 2. Sign In
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "ananya.sharma@example.com",
    "password": "SecurePassword123!"
  }'
```
Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "usr_9b2e4f...",
  "displayName": "Ananya Sharma",
  "email": "ananya.sharma@example.com",
  "role": "MEMBER",
  "mfaRequired": false
}
```

### 3. Fetch My Profile
```bash
curl -X GET http://localhost:8080/api/v1/profile \
  -H "Authorization: Bearer <TOKEN>"
```

### 4. Search Matches
```bash
curl -X GET "http://localhost:8080/api/v1/discovery/search?gender=Female&city=Bangalore&page=0&size=10" \
  -H "Authorization: Bearer <TOKEN>"
```

### 5. Logout & Revoke Session
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <TOKEN>"
```
