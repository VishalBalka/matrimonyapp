# MatrimonyApp API Documentation Index

## Overview

MatrimonyApp provides a security-hardened, RESTful API architecture for matrimony and matchmaking services.
All sensitive interactions require cryptographic bearer token authentication, server-side authorization enforcement, rate-limiting, and structured audit event emission.

## API Documentation Catalog

1. [Auth API](AUTH_API.md) - Account registration, login, session termination, token renewal.
2. [OTP API](OTP_API.md) - Cryptographic OTP generation, rate limits, single-use verification.
3. [MFA API](MFA_API.md) - Two-factor authentication enrollment, TOTP verification, recovery codes.
4. [User API](USER_API.md) - Current identity resolution (`/me`), role inspection.
5. [Profile API](PROFILE_API.md) - Private profile CRUD, photo upload/deletion, visibility flags.
6. [Search API](SEARCH_API.md) - Privacy-safe discovery queries, age range filtering, geolocation search.
7. [Notification API](NOTIFICATION_API.md) - Notification retrieval, unread indicators, read state updates.
8. [Security API](SECURITY_API.md) - Privacy settings, user block/unblock, abuse reporting.
9. [Admin API](ADMIN_API.md) - Privileged operations, dashboard metrics, report moderation, account lock.
10. [Session API](SESSION_API.md) - Active session tracking, token revocation, multi-device management.
11. [Error Codes](ERROR_CODES.md) - Machine-readable error codes and HTTP mapping.
12. [Rate Limits](RATE_LIMITS.md) - Multi-dimensional rate limit specifications.
13. [Security Model](SECURITY_MODEL.md) - Trust boundaries, defense-in-depth principles.
14. [Postman Guide](POSTMAN_GUIDE.md) - Instructions for automated and manual testing via Postman.
15. [Endpoint Catalog](API_ENDPOINT_CATALOG.md) - Exhaustive endpoint specification.
