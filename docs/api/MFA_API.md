# MFA & Step-Up Security API

## 1. Setup MFA (Enrollment)
- **Method:** `POST`
- **Path:** `/api/v1/security/mfa/setup`
- **Auth Required:** Bearer Token
- **Rate Limit:** 3 requests / 15 minutes
- **Response (200 OK):**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "message": "Scan the QR code or enter this secret into your authenticator app."
}
```

## 2. Enable MFA
- **Method:** `POST`
- **Path:** `/api/v1/security/mfa/enable`
- **Auth Required:** Bearer Token
- **Request Body:**
```json
{
  "code": "123456"
}
```
- **Response (200 OK):**
```json
{
  "message": "Two-factor authentication enabled."
}
```

## 3. Disable MFA
- **Method:** `POST`
- **Path:** `/api/v1/security/mfa/disable`
- **Auth Required:** Bearer Token + recent step-up code
- **Request Body:**
```json
{
  "code": "123456"
}
```
- **Response (200 OK):**
```json
{
  "message": "Two-factor authentication disabled."
}
```
