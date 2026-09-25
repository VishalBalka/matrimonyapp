# API Troubleshooting & Diagnostic Guide

## 1. Authentication Failure Breakdown (POST `/api/v1/auth/login`)

| Stage | Checkpoint | Indicator | Resolution |
|---|---|---|---|
| 1. Ingress | Request payload reached Gateway | Correlation ID emitted | Ensure client TLS handshake succeeds |
| 2. Validation | Email format and password non-empty | `VALIDATION_FAILED` (400) | Check email syntax and password presence |
| 3. Rate Limit | Bucket evaluation for IP & Email | `RATE_LIMITED` (429) | Check Redis/In-memory rate limit logs |
| 4. User Lookup | Query user record by normalized email | `AUTH_INVALID_CREDENTIALS` | Generic error response to prevent user enumeration |
| 5. Password Check | Adaptive BCrypt/Argon2 verification | Security audit event logged | Verify hash matches salt specification |
| 6. MFA Check | Check if user enrolled in TOTP | Returns `mfaRequired: true` | Client must route to `/api/v1/auth/mfa/verify` |
| 7. Session Issuance | Generate cryptographically signed token | Token stored in SecureTokenStore | Inspect token validity timestamp |

## 2. Profile Sync & Update (PUT `/api/v1/profile`)

- **Issue:** 400 Bad Request
  - Check `validateProfile` constraints: Date of birth must be YYYY-MM-DD, age >= 18 and <= 100, Bio <= 500 characters, Phone <= 24 characters.
- **Issue:** 401 Unauthorized
  - Bearer token expired or revoked. Re-authenticate via `/api/v1/auth/login`.
