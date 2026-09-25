# OTP (One-Time Password) API

## Overview
The OTP system provides cryptographically random 6-digit numeric verification tokens with short lifespans (5 minutes), attempt limits (maximum 3 attempts), and cryptographic hashing storage.

## 1. Request OTP
- **Method:** `POST`
- **Path:** `/api/v1/auth/otp/request`
- **Auth Required:** Optional (supports pre-login verification and password resets)
- **Rate Limit:** 3 requests / 10 minutes / account
- **Request Body:**
```json
{
  "email": "user@example.com",
  "purpose": "REGISTRATION_VERIFICATION" // or "PASSWORD_RESET", "STEP_UP"
}
```
- **Response (200 OK):**
```json
{
  "message": "If an account exists, a one-time verification code has been dispatched.",
  "cooldownSeconds": 60
}
```

## 2. Verify OTP
- **Method:** `POST`
- **Path:** `/api/v1/auth/otp/verify`
- **Auth Required:** No
- **Rate Limit:** 5 attempts / 5 minutes / IP
- **Request Body:**
```json
{
  "email": "user@example.com",
  "code": "489201",
  "purpose": "REGISTRATION_VERIFICATION"
}
```
- **Response (200 OK):**
```json
{
  "verified": true,
  "verificationToken": "temp-auth-verified-token",
  "message": "Code verified successfully."
}
```
- **Error Responses:**
  - `400 Bad Request` - `OTP_INVALID` ("Invalid verification code")
  - `410 Gone` - `OTP_EXPIRED` ("Verification code has expired")
  - `429 Too Many Requests` - `OTP_TOO_MANY_ATTEMPTS` ("Attempt limit exceeded")
