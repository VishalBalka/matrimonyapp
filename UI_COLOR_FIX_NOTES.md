# UI Color / Contrast Fix

This pass addresses the washed-out text shown in the Android screenshots.

## Changes

- Strengthened primary and secondary text colors for WCAG-friendly contrast.
- Reduced the white wash over the cloud background while retaining a subtle pastel visual.
- Made all profile form fields use the same explicit Material 3 field colors.
- Added a darker focused field border and consistent cursor/icon colors.
- Updated Home cards and actions to use navy text and the maroon action color.
- Updated Search colors to use the shared application palette.
- Changed bottom navigation selected state to a maroon filled pill with white icon/text.
- Replaced the Matches placeholder with a readable empty state.
- Login and Registration now use the shared background treatment instead of covering it with a flat layer.

## Physical-device debug build

Use the PC LAN API URL when building for the physical Android device:

```powershell
.\gradlew.bat :app:clean :app:testDebugUnitTest :app:assembleDebug `
  -PMATRIMONY_API_BASE_URL=http://<PC-LAN-IP>:8080/ `
  --no-daemon
```

Then install:

```powershell
adb install -r .\app\build\outputs\apk\debug\app-debug.apk
```

