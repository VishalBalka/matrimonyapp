# Security, Privacy & Admin APIs

## 1. Security State & Privacy (`/api/v1/security`)
- **GET** `/api/v1/security`: Returns MFA status, profile visibility, lock state, and privacy indicators.
- **PUT** `/api/v1/security/privacy`: Update privacy toggles (`visibility`, `profileLocked`, `showPhone`, `showSalary`, `showSocial`).
- **POST** `/api/v1/security/block/{userId}`: Block a member from seeing or contacting profile.
- **DELETE** `/api/v1/security/block/{userId}`: Unblock a member.
- **POST** `/api/v1/security/report/{userId}`: Submit an abuse report with reason code and details.

## 2. Notification System (`/api/v1/notifications`)
- **GET** `/api/v1/notifications`: List recent notifications (profile views, match alerts, verification notices).
- **POST** `/api/v1/notifications/{id}/read`: Mark specific notification as read.
- **POST** `/api/v1/notifications/read-all`: Mark all notifications as read.

## 3. Administrative Control (`/api/v1/admin`)
- **GET** `/api/v1/admin/dashboard`: Platform metrics (users, profiles, open reports, pending verifications).
- **GET** `/api/v1/admin/reports?status=OPEN`: Moderation queue.
- **POST** `/api/v1/admin/profiles/{profileId}/verify`: Grant/revoke verified status badge.
- **POST** `/api/v1/admin/profiles/{profileId}/background-check`: Set verification status (`PENDING`, `VERIFIED`, `REJECTED`).
- **POST** `/api/v1/admin/reports/{reportId}/status`: Update report state (`INVESTIGATING`, `RESOLVED`, `DISMISSED`).
- **POST** `/api/v1/admin/users/{userId}/lock`: Emergency administrative account lock.
