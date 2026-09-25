# Production readiness notes

This branch contains a security and production-hardening pass over the current Android + Spring Boot/PostgreSQL application.

## Deployment requirements

- Set `SPRING_PROFILES_ACTIVE=prod` for the backend.
- Supply `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` from a secret manager.
- Supply a 32-byte base64 `MFA_ENCRYPTION_KEY`.
- Set `CORS_ALLOWED_ORIGINS` only to trusted HTTPS web origins. Leave empty for the Android-only API.
- Supply `ADMIN_EMAIL` and `ADMIN_PASSWORD` only when intentionally bootstrapping an administrator.
- Put the API behind TLS. The Android release build rejects non-HTTPS API URLs.
- Debug Android builds target the local emulator at `http://10.0.2.2:8080/`. Override with `-PMATRIMONY_API_BASE_URL=...` when needed.
- Build Android release with `-PMATRIMONY_API_BASE_URL=https://<your-api-host>/`.

## Security controls added or tightened

- Security response headers: CSP, HSTS on TLS requests, frame denial, MIME sniffing protection, referrer policy, permissions policy, and no-store caching.
- Explicit CORS allow-list instead of wildcard origins.
- Bounded bearer-token parsing and request IDs.
- Bounded/cleaned in-memory authentication rate-limit state to reduce memory exhaustion from attacker-controlled keys.
- SSE connections are bounded per user and have a finite lifetime.
- Discovery/session/MFA indexes were added for production query paths.
- Release Android builds use R8/resource shrinking and require HTTPS.
- The debug network policy no longer whitelists the hard-coded LAN address from the original project.

## Verification limitation

The uploaded environment did not contain a system Gradle installation, and the Gradle wrapper could not download its distribution because outbound network access was unavailable. Therefore the final artifact includes the code changes and static checks, but a clean-room Gradle build/test run still needs to be executed in CI or a connected build environment.
