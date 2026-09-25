# MatrimonyApp

A modern Android matrimony and matchmaking application built with Kotlin and Jetpack Compose.

## Features

- **Authentication & Security:** Secure token storage, email/password login, account registration, biometric/device lock integration, and optional MFA support.
- **Profile Discovery & Search:** Search profiles by age ranges, locations (city, state, country), and view verified badges, career details, education, and social links.
- **Detailed Profiles:** In-depth profile views featuring verified identity badges, professions, skills, and background check statuses.
- **My Profile Management:** Manage personal bio, career info, contact details, and granular privacy controls (visibility toggles for phone, salary, social links).
- **Notifications:** Real-time updates for profile visits, match recommendations, and verification milestones.
- **Admin Dashboard:** Moderation console with user verification, report handling, and account status controls for administrator accounts.
- **Offline & Standalone Support:** Includes seamless local fallback handling for standalone emulator use and testing.

## Tech Stack

- **UI:** Jetpack Compose with Material 3 design and adaptive layouts.
- **Networking:** Retrofit, OkHttp, Gson.
- **Architecture:** MVVM with Kotlin Coroutines and StateFlow.
- **Platform:** Android SDK 37, AGP 9.1.1, Kotlin 2.4.20.
