# Threat Model & Risk Analysis (STRIDE Framework)

| Category | Threat Scenario | Mitigation Strategy |
|---|---|---|
| **Spoofing** | Credential stuffing & fake authentication | Adaptive password hashing, brute-force rate limits, optional TOTP MFA |
| **Tampering** | Intercepting and altering profile requests | Strict TLS 1.3 encryption, server-side payload validation |
| **Repudiation** | Denying an administrative moderation action | Immutable, structured audit logging with actor IDs and timestamps |
| **Information Disclosure** | Scraping member phone numbers or locations | Strict privacy filters, phone number masking unless opted-in |
| **Denial of Service** | Flooding authentication or search endpoints | Multi-dimensional IP + account sliding window rate limiting |
| **Elevation of Privilege** | User impersonating an admin | Server-side role resolution from cryptographic token only |
