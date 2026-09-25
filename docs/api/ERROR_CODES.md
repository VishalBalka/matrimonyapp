# MatrimonyApp Standard Error Codes

All errors adhere to the unified envelope:
```json
{
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Unable to authenticate with the supplied credentials.",
    "requestId": "req-98f2-31ad"
  }
}
```

| HTTP Status | Error Code | Description |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Input syntax or constraint violation |
| 400 | `BAD_REQUEST` | Malformed JSON or invalid parameter syntax |
| 400 | `OTP_INVALID` | Invalid verification code |
| 401 | `UNAUTHORIZED` | Missing or invalid Bearer token |
| 401 | `AUTH_INVALID_CREDENTIALS` | Invalid email/password pair |
| 401 | `SESSION_EXPIRED` | Access token has expired |
| 403 | `ACCESS_DENIED` | Insufficient role or unauthorized resource access |
| 403 | `ACCOUNT_LOCKED` | Account has been locked by administrator or rate protection |
| 404 | `RESOURCE_NOT_FOUND` | Profile, notification, or report does not exist |
| 409 | `CONFLICT` | Resource already exists (e.g. registered email) |
| 410 | `OTP_EXPIRED` | The supplied OTP has expired |
| 429 | `RATE_LIMITED` | Rate limit quota exceeded |
| 429 | `OTP_TOO_MANY_ATTEMPTS` | Exceeded maximum attempts for OTP code |
| 500 | `INTERNAL_ERROR` | Sanitized unexpected server error |
