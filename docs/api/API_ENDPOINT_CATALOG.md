# Complete Backend API Endpoint Catalog

| Method | Path | Controller | Auth Required | Role | Purpose | Database Tables |
|---|---|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | `AuthController` | No | Any | User account registration | `users` |
| `POST` | `/api/v1/auth/login` | `AuthController` | No | Any | Credential authentication & session issuance | `users`, `sessions` |
| `POST` | `/api/v1/auth/mfa/verify` | `AuthController` | Challenge | Any | Complete 2FA challenge | `users`, `sessions` |
| `POST` | `/api/v1/auth/logout` | `AuthController` | Bearer | Any | Terminate active token/session | `sessions` |
| `GET` | `/api/v1/auth/me` | `AuthController` | Bearer | Any | Resolve authenticated caller principal | `users` |
| `GET` | `/api/v1/profile` | `ProfileController` | Bearer | Any | Fetch owner profile | `profiles` |
| `PUT` | `/api/v1/profile` | `ProfileController` | Bearer | Any | Update profile details and visibility | `profiles` |
| `POST` | `/api/v1/profile/photo` | `ProfileController` | Bearer | Any | Upload profile photo multipart | `profiles`, `photos` |
| `DELETE` | `/api/v1/profile/photo` | `ProfileController` | Bearer | Any | Remove profile photo | `profiles`, `photos` |
| `GET` | `/api/v1/profiles/search` | `DiscoveryController` | Bearer | Any | Query discoverable profiles | `profiles` |
| `GET` | `/api/v1/profiles/{id}` | `DiscoveryController` | Bearer | Any | Read discoverable profile details | `profiles` |
| `GET` | `/api/v1/notifications` | `NotificationController`| Bearer | Any | List user notifications | `notifications` |
| `POST` | `/api/v1/notifications/{id}/read` | `NotificationController` | Bearer | Any | Mark notification read | `notifications` |
| `GET` | `/api/v1/security` | `SecurityController` | Bearer | Any | Retrieve security & privacy status | `users`, `profiles` |
| `PUT` | `/api/v1/security/privacy`| `SecurityController`| Bearer | Any | Update privacy flags | `profiles` |
| `POST` | `/api/v1/security/mfa/setup`| `SecurityController`| Bearer | Any | Generate TOTP secret | `users` |
| `POST` | `/api/v1/security/mfa/enable`| `SecurityController`| Bearer | Any | Confirm and enable TOTP MFA | `users` |
| `POST` | `/api/v1/security/mfa/disable`| `SecurityController`| Bearer | Any | Disable TOTP MFA | `users` |
| `POST` | `/api/v1/security/block/{id}` | `SecurityController` | Bearer | Any | Block target member | `blocks` |
| `DELETE`| `/api/v1/security/block/{id}` | `SecurityController` | Bearer | Any | Unblock target member | `blocks` |
| `POST` | `/api/v1/security/report/{id}` | `SecurityController` | Bearer | Any | File abuse report | `reports` |
| `GET` | `/api/v1/admin/dashboard` | `AdminController` | Bearer | ADMIN | View system metrics & queues | `users`, `profiles`, `reports` |
| `GET` | `/api/v1/admin/reports` | `AdminController` | Bearer | ADMIN | Review moderation queue | `reports` |
| `POST` | `/api/v1/admin/profiles/{id}/verify` | `AdminController` | Bearer | ADMIN | Toggle profile verification badge | `profiles` |
| `POST` | `/api/v1/admin/profiles/{id}/background-check` | `AdminController` | Bearer | ADMIN | Update background check status | `profiles` |
| `POST` | `/api/v1/admin/reports/{id}/status` | `AdminController` | Bearer | ADMIN | Resolve/dismiss abuse report | `reports` |
| `POST` | `/api/v1/admin/users/{id}/lock` | `AdminController` | Bearer | ADMIN | Lock/suspend user account | `users`, `sessions` |
