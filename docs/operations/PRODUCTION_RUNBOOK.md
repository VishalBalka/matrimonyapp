# Production Runbook

## 1. Health Checks
- **Liveness:** `GET /api/v1/health` -> Returns `200 OK`
- **Database Connectivity:** Evaluated via connection pool validation query.

## 2. Emergency Account Lock
To immediately terminate all active sessions and block an account:
```bash
curl -X POST "https://api.matrimony.example.com/api/v1/admin/users/{userId}/lock" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 3. Emergency Token & Session Revocation
- Invalidate session records in the database.
- Rotate backend JWT signing secrets if compromise is suspected.

## 4. Backups and Restores
- Automated point-in-time PostgreSQL WAL archiving.
- Daily encrypted snapshots stored with strict IAM separation.
