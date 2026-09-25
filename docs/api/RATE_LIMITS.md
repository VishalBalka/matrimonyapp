# Rate Limiting & Abuse Prevention Model

| Endpoint | Dimension | Rate Limit | Cooldown |
|---|---|---|---|
| `/api/v1/auth/login` | IP + Email | 5 attempts / 15 minutes | 15 minutes lockout |
| `/api/v1/auth/register` | IP | 3 registrations / hour | 60 minutes |
| `/api/v1/auth/otp/request` | User Identifier | 3 requests / 10 minutes | 60 seconds per request |
| `/api/v1/auth/otp/verify` | User + IP | 5 attempts / 5 minutes | Invalidation after 5 tries |
| `/api/v1/auth/mfa/verify` | Challenge ID | 5 attempts / 5 minutes | Invalidation of challenge |
| `/api/v1/profiles/search` | Bearer Token | 60 queries / minute | Sliding window |
| `/api/v1/security/privacy` | Bearer Token | 10 updates / minute | Sliding window |
| `/api/v1/admin/*` | Admin Token | 120 requests / minute | Sliding window |

Rate-limit headers returned:
- `X-RateLimit-Limit`: Maximum allowable requests in period
- `X-RateLimit-Remaining`: Remaining request quota
- `Retry-After`: Seconds to wait before subsequent requests when HTTP 429 is encountered
