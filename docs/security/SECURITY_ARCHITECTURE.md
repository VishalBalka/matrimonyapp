# MatrimonyApp Security Architecture

## Defense-in-Depth Model

1. **Edge Protection:** TLS 1.3 encryption, strict CORS, rate limiting, and request sanitization.
2. **Identity & Authentication:** Adaptive password hashing, single-use cryptographically random OTPs, TOTP multi-factor authentication, tamper-evident Bearer tokens.
3. **Authorization Layer:** Server-side role-based access control (RBAC), strict ownership verification on all resource modifications (preventing BOLA/IDOR).
4. **Android Client Hardening:** Encrypted SharedPreferences / Android Keystore, certificate pinning in production, zero plaintext secret storage, automatic screen credential app locking.
5. **Auditing & Observability:** Cryptographic request correlation IDs (`X-Request-ID`), structured security event auditing, automatic log redaction of sensitive credentials.
