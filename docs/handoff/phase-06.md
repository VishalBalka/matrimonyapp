\# Phase 06 - Android/backend Integration



\## Status



Phase 06 verification passed. Android unit tests and debug APK build passed. Physical-device authentication was verified.



\## Scope



\- Android Retrofit authentication client

\- Registration

\- Login

\- Authenticated `/me`

\- Logout

\- Keystore-backed token storage

\- 401 session invalidation

\- API error mapping

\- Android authentication state

\- Physical-device LAN connectivity

\- Emulator connectivity through `10.0.2.2`

\- Cloud/glass Android UI

\- Authenticated glass profile screen

\- Floating glass navigation



\## Backend



Local backend uses Docker PostgreSQL through host port `55432`.



Spring Boot runs on port `8080`.



Database migrations validated successfully at schema version 2.



\## Network Configuration



Debug Android networking permits:



\- `10.0.2.2` for Android emulator

\- `192.168.29.82` for the verified physical development phone



The debug network configuration fails closed by default.



\## Verification



\### Android unit tests



Command:



```powershell

cd V:\\Projects\\MatrimonyApp\\android

.\\gradlew.bat :app:testDebugUnitTest
