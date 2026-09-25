# Production Debugging & Incident Diagnostics

## Tracing Requests via Correlation ID
Every inbound API request receives a unique `X-Request-ID` (UUIDv4) header.
Clients receive this identifier in error responses:
```json
{
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Unable to authenticate with the supplied credentials.",
    "requestId": "req-8f4b-21d4"
  }
}
```

### Log Query Pattern
To isolate an incident in production logs:
```bash
grep "req-8f4b-21d4" /var/log/matrimony/application.log
```

## Production Safe Error Masking
- Raw SQL exceptions, table names, and stack traces are suppressed in HTTP responses.
- All 500 errors log the full stack trace internally under the correlation ID while returning `INTERNAL_ERROR` to the caller.
