# OTP & MFA Security Specifications

## OTP Security
1. **Entropy:** 6-digit numeric codes generated using `java.security.SecureRandom`.
2. **Expiration:** Strictly 5 minutes from generation.
3. **Single-Use:** Once verified or after 3 failed attempts, the code is immediately invalidated.
4. **Hashed Storage:** Never stored in plaintext; stored as a cryptographic hash.
5. **Cooldown:** 60-second cooldown between resend requests.

## MFA Security
1. **TOTP Standards:** RFC 6238 compliant TOTP using SHA-1/SHA-256 with 30-second time steps.
2. **Secret Storage:** Encrypted at rest using AES-256-GCM.
3. **Step-Up Verification:** Critical security modifications (disabling MFA, changing password) require recent step-up authentication.
