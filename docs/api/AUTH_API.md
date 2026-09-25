# Authentication API

## 1. Register Account
- **Method:** `POST`
- **Path:** `/api/v1/auth/register`
- **Auth Required:** No
- **Rate Limit:** 5 requests / minute / IP
- **Request Body:**
```json
{
  "displayName": "Aditi Rao",
  "email": "aditi.rao@example.com",
  "password": "StrongPassword123!",
  "confirmPassword": "StrongPassword123!"
}
```
- **Response (200 OK):**
```json
{
  "userId": "user-a8b29c11",
  "message": "Account created successfully."
}
```

## 2. Login
- **Method:** `POST`
- **Path:** `/api/v1/auth/login`
- **Auth Required:** No
- **Rate Limit:** 10 requests / 5 minutes / IP & email
- **Request Body:**
```json
{
  "email": "aditi.rao@example.com",
  "password": "StrongPassword123!"
}
```
- **Response (200 OK - Standard):**
```json
{
  "token": "bearer-token-uuid",
  "userId": "user-a8b29c11",
  "displayName": "Aditi Rao",
  "email": "aditi.rao@example.com",
  "expiresAt": "2030-12-31T23:59:59Z",
  "mfaRequired": false,
  "mfaChallengeId": null,
  "role": "MEMBER"
}
```
- **Response (200 OK - MFA Challenge Pending):**
```json
{
  "token": "",
  "userId": "user-a8b29c11",
  "displayName": "",
  "email": "aditi.rao@example.com",
  "expiresAt": "",
  "mfaRequired": true,
  "mfaChallengeId": "mfa-chal-9218",
  "role": ""
}
```

## 3. Verify MFA
- **Method:** `POST`
- **Path:** `/api/v1/auth/mfa/verify`
- **Auth Required:** No (Challenge ID verified)
- **Request Body:**
```json
{
  "challengeId": "mfa-chal-9218",
  "code": "123456"
}
```
- **Response (200 OK):** `LoginResponseDto`

## 4. Logout
- **Method:** `POST`
- **Path:** `/api/v1/auth/logout`
- **Auth Required:** Bearer Token
- **Response (200 OK):**
```json
{
  "message": "Logout successful."
}
```
