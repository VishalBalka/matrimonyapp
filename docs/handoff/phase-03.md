# Phase 03 Handoff

## Scope
Login and registration UI, client-side validation, error states, and navigation between authentication screens.

## Constraints
No backend calls, OTP, social login, MFA/2FA, or password reset. Password contents are not trimmed, lowercased, normalized, or silently modified.

## Verification
Run `scripts/verify-phase-03.ps1` and `android/gradlew.bat test`, then build the debug APK. Record actual output before committing.
